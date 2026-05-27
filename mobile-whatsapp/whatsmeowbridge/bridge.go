package whatsmeowbridge

import (
	"context"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	_ "modernc.org/sqlite"

	"go.mau.fi/whatsmeow"
	waProto "go.mau.fi/whatsmeow/binary/proto"
	"go.mau.fi/whatsmeow/store/sqlstore"
	"go.mau.fi/whatsmeow/types"
	"go.mau.fi/whatsmeow/types/events"
	"google.golang.org/protobuf/proto"
)

type Listener interface {
	OnState(state string, detail string)
	OnPairingCode(code string)
	OnMessage(id string, chatID string, senderName string, text string, quotedMessageID string, quotedText string, timestampMillis int64)
	OnError(message string)
}

type incomingMedia struct {
	mediaType string
	path      string
	mime      string
	fileName  string
}

type Bridge struct {
	mu        sync.Mutex
	rootDir   string
	listener  Listener
	container *sqlstore.Container
	client    *whatsmeow.Client
	media     map[string]incomingMedia
}

func NewBridge(rootDir string, listener Listener) *Bridge {
	return &Bridge{rootDir: rootDir, listener: listener, media: make(map[string]incomingMedia)}
}

func (b *Bridge) Start() error {
	b.mu.Lock()
	defer b.mu.Unlock()

	client, err := b.clientLocked(false)
	if err != nil {
		b.emitError(err)
		return err
	}
	if client.IsConnected() {
		b.emitState("running", "")
		return nil
	}
	if client.Store.ID == nil {
		b.emitState("waiting_pairing", "Generate a pairing code in Settings.")
		return nil
	}
	b.emitState("connecting", "")
	if err = client.Connect(); err != nil {
		b.emitError(err)
		return err
	}
	b.emitState("running", "")
	return nil
}

func (b *Bridge) PairPhone(phone string) (string, error) {
	b.mu.Lock()
	defer b.mu.Unlock()

	phone = onlyDigits(phone)
	if len(phone) < 10 {
		return "", errors.New("invalid phone number")
	}
	client, err := b.clientLocked(true)
	if err != nil {
		b.emitError(err)
		return "", err
	}
	if client.IsConnected() && client.Store.ID != nil {
		b.emitState("running", "")
		return "ALREADY-LINKED", nil
	}

	qrChan, err := client.GetQRChannel(context.Background())
	if err != nil {
		b.emitError(err)
		return "", err
	}
	b.emitState("connecting", "")
	if err = client.Connect(); err != nil {
		b.emitError(err)
		return "", err
	}

	select {
	case <-qrChan:
	case <-time.After(12 * time.Second):
		return "", errors.New("timed out waiting for pairing channel")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	code, err := client.PairPhone(ctx, phone, true, whatsmeow.PairClientChrome, "Chrome (Linux)")
	if err != nil {
		b.emitError(err)
		return "", err
	}
	if b.listener != nil {
		b.listener.OnPairingCode(code)
	}
	return code, nil
}

func (b *Bridge) Stop() {
	b.mu.Lock()
	defer b.mu.Unlock()
	if b.client != nil {
		b.client.Disconnect()
	}
	b.emitState("disconnected", "")
}

func (b *Bridge) ClearSession() error {
	b.Stop()
	b.mu.Lock()
	defer b.mu.Unlock()
	if b.container != nil {
		_ = b.container.Close()
		b.container = nil
	}
	b.client = nil
	err := os.RemoveAll(filepath.Join(b.rootDir, "whatsmeow.db"))
	if err != nil {
		b.emitError(err)
	}
	return err
}

func (b *Bridge) SendText(chatID string, text string) (string, error) {
	b.mu.Lock()
	client := b.client
	b.mu.Unlock()
	if client == nil || !client.IsConnected() {
		return "", errors.New("whatsapp is not connected")
	}
	jid, err := types.ParseJID(chatID)
	if err != nil {
		return "", err
	}
	ctx, cancel := context.WithTimeout(context.Background(), 45*time.Second)
	defer cancel()
	resp, err := client.SendMessage(ctx, jid, &waProto.Message{Conversation: proto.String(text)})
	return resp.ID, err
}

func (b *Bridge) SendTextToGroupName(groupName string, text string) (string, error) {
	b.mu.Lock()
	client := b.client
	b.mu.Unlock()
	if client == nil || !client.IsConnected() {
		return "", errors.New("whatsapp is not connected")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	groups, err := client.GetJoinedGroups(ctx)
	if err != nil {
		return "", err
	}
	for _, group := range groups {
		if strings.EqualFold(strings.TrimSpace(group.Name), strings.TrimSpace(groupName)) {
			ctx, sendCancel := context.WithTimeout(context.Background(), 45*time.Second)
			defer sendCancel()
			resp, err := client.SendMessage(ctx, group.JID, &waProto.Message{Conversation: proto.String(text)})
			return resp.ID, err
		}
	}
	return "", fmt.Errorf("group not found: %s", groupName)
}

func (b *Bridge) SendMedia(chatID string, path string, caption string, mime string) error {
	b.mu.Lock()
	client := b.client
	b.mu.Unlock()
	if client == nil || !client.IsConnected() {
		return errors.New("whatsapp is not connected")
	}
	jid, err := types.ParseJID(chatID)
	if err != nil {
		return err
	}
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	if mime == "" {
		mime = mimeFromPath(path)
	}
	mediaType := whatsmeow.MediaDocument
	if strings.HasPrefix(mime, "video/") {
		mediaType = whatsmeow.MediaVideo
	} else if strings.HasPrefix(mime, "audio/") {
		mediaType = whatsmeow.MediaAudio
	} else if strings.HasPrefix(mime, "image/") {
		mediaType = whatsmeow.MediaImage
	}
	ctx, cancel := context.WithTimeout(context.Background(), 120*time.Second)
	defer cancel()
	upload, err := client.Upload(ctx, data, mediaType)
	if err != nil {
		if mediaType == whatsmeow.MediaVideo && isUploadTooLarge(err) {
			mediaType = whatsmeow.MediaDocument
			mime = "application/octet-stream"
			upload, err = client.Upload(ctx, data, mediaType)
		}
		if err != nil {
			return err
		}
	}
	message := mediaMessage(mediaType, upload, caption, filepath.Base(path), mime)
	_, err = client.SendMessage(ctx, jid, message)
	return err
}

func (b *Bridge) SendSticker(chatID string, path string) error {
	b.mu.Lock()
	client := b.client
	b.mu.Unlock()
	if client == nil || !client.IsConnected() {
		return errors.New("whatsapp is not connected")
	}
	jid, err := types.ParseJID(chatID)
	if err != nil {
		return err
	}
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	ctx, cancel := context.WithTimeout(context.Background(), 120*time.Second)
	defer cancel()
	upload, err := client.Upload(ctx, data, whatsmeow.MediaImage)
	if err != nil {
		return err
	}
	_, err = client.SendMessage(ctx, jid, &waProto.Message{StickerMessage: &waProto.StickerMessage{
		Mimetype:      proto.String("image/webp"),
		URL:           proto.String(upload.URL),
		DirectPath:    proto.String(upload.DirectPath),
		MediaKey:      upload.MediaKey,
		FileEncSHA256: upload.FileEncSHA256,
		FileSHA256:    upload.FileSHA256,
		FileLength:    proto.Uint64(upload.FileLength),
		IsAnimated:    proto.Bool(false),
	}})
	return err
}

func (b *Bridge) MediaTypeForMessage(id string) string {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.media[id].mediaType
}

func (b *Bridge) MediaPathForMessage(id string) string {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.media[id].path
}

func (b *Bridge) MediaMimeForMessage(id string) string {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.media[id].mime
}

func (b *Bridge) MediaFileNameForMessage(id string) string {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.media[id].fileName
}

func (b *Bridge) CleanupMediaForMessage(id string) {
	b.mu.Lock()
	media := b.media[id]
	delete(b.media, id)
	b.mu.Unlock()
	if media.path != "" {
		_ = os.Remove(media.path)
	}
}

func isUploadTooLarge(err error) bool {
	if err == nil {
		return false
	}
	message := strings.ToLower(err.Error())
	return strings.Contains(message, "413") || strings.Contains(message, "too large") || strings.Contains(message, "request entity")
}

func (b *Bridge) clientLocked(reset bool) (*whatsmeow.Client, error) {
	if b.client != nil && !reset {
		return b.client, nil
	}
	if err := os.MkdirAll(b.rootDir, 0700); err != nil {
		return nil, err
	}
	b.cleanupOldMediaCache()
	container, err := sqlstore.New(context.Background(), "sqlite", "file:"+filepath.Join(b.rootDir, "whatsmeow.db")+"?_pragma=foreign_keys(1)", nil)
	if err != nil {
		return nil, err
	}
	device, err := container.GetFirstDevice(context.Background())
	if err != nil {
		return nil, err
	}
	if device == nil || reset {
		device = container.NewDevice()
	}
	client := whatsmeow.NewClient(device, nil)
	client.AddEventHandler(b.handleEvent)
	b.container = container
	b.client = client
	return client, nil
}

func (b *Bridge) handleEvent(evt interface{}) {
	switch event := evt.(type) {
	case *events.Connected:
		b.emitState("running", "")
	case *events.Disconnected:
		b.emitState("disconnected", "")
	case *events.LoggedOut:
		b.emitState("disconnected", "logged out")
	case *events.Message:
		if event.Info.IsFromMe {
			return
		}
		text := messageText(event.Message)
		media := b.cacheIncomingImage(event.Info.ID, event.Message, false)
		if quotedID := quotedMessageID(event.Message); quotedID != "" {
			_ = b.cacheIncomingImage(quotedID, quotedMessage(event.Message), true)
		}
		if strings.TrimSpace(text) == "" && media.path == "" {
			return
		}
		if b.listener != nil {
			b.listener.OnMessage(event.Info.ID, event.Info.Chat.String(), event.Info.PushName, text, quotedMessageID(event.Message), quotedMessageText(event.Message), event.Info.Timestamp.UnixMilli())
		}
	}
}

func (b *Bridge) cacheIncomingImage(id string, message *waProto.Message, quiet bool) incomingMedia {
	if id == "" || message == nil || message.GetImageMessage() == nil {
		return incomingMedia{}
	}
	image := message.GetImageMessage()
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()
	data, err := b.client.Download(ctx, image)
	if err != nil {
		if !quiet {
			b.emitError(fmt.Errorf("failed to download image message: %w", err))
		}
		return incomingMedia{}
	}
	mime := image.GetMimetype()
	if mime == "" {
		mime = "image/jpeg"
	}
	fileName := safeMediaFileName(id, mime)
	cacheDir := filepath.Join(b.rootDir, "media-cache")
	if err = os.MkdirAll(cacheDir, 0700); err != nil {
		b.emitError(err)
		return incomingMedia{}
	}
	path := filepath.Join(cacheDir, fileName)
	if err = os.WriteFile(path, data, 0600); err != nil {
		b.emitError(err)
		return incomingMedia{}
	}
	media := incomingMedia{mediaType: "image", path: path, mime: mime, fileName: fileName}
	b.mu.Lock()
	b.media[id] = media
	b.mu.Unlock()
	return media
}

func messageText(message *waProto.Message) string {
	if message == nil {
		return ""
	}
	if message.GetConversation() != "" {
		return message.GetConversation()
	}
	if extended := message.GetExtendedTextMessage(); extended != nil {
		return extended.GetText()
	}
	if image := message.GetImageMessage(); image != nil {
		return image.GetCaption()
	}
	if video := message.GetVideoMessage(); video != nil {
		return video.GetCaption()
	}
	if document := message.GetDocumentMessage(); document != nil {
		return document.GetCaption()
	}
	return ""
}

func quotedMessageID(message *waProto.Message) string {
	if message == nil {
		return ""
	}
	if extended := message.GetExtendedTextMessage(); extended != nil && extended.GetContextInfo() != nil {
		return extended.GetContextInfo().GetStanzaID()
	}
	if image := message.GetImageMessage(); image != nil && image.GetContextInfo() != nil {
		return image.GetContextInfo().GetStanzaID()
	}
	if video := message.GetVideoMessage(); video != nil && video.GetContextInfo() != nil {
		return video.GetContextInfo().GetStanzaID()
	}
	if document := message.GetDocumentMessage(); document != nil && document.GetContextInfo() != nil {
		return document.GetContextInfo().GetStanzaID()
	}
	return ""
}

func quotedMessageText(message *waProto.Message) string {
	return messageText(quotedMessage(message))
}

func quotedMessage(message *waProto.Message) *waProto.Message {
	if message == nil {
		return nil
	}
	if extended := message.GetExtendedTextMessage(); extended != nil && extended.GetContextInfo() != nil {
		return extended.GetContextInfo().GetQuotedMessage()
	}
	if image := message.GetImageMessage(); image != nil && image.GetContextInfo() != nil {
		return image.GetContextInfo().GetQuotedMessage()
	}
	if video := message.GetVideoMessage(); video != nil && video.GetContextInfo() != nil {
		return video.GetContextInfo().GetQuotedMessage()
	}
	if document := message.GetDocumentMessage(); document != nil && document.GetContextInfo() != nil {
		return document.GetContextInfo().GetQuotedMessage()
	}
	return nil
}

func mediaMessage(mediaType whatsmeow.MediaType, upload whatsmeow.UploadResponse, caption, fileName, mime string) *waProto.Message {
	switch mediaType {
	case whatsmeow.MediaVideo:
		return &waProto.Message{VideoMessage: &waProto.VideoMessage{
			Caption:       proto.String(caption),
			Mimetype:      proto.String(mime),
			URL:           proto.String(upload.URL),
			DirectPath:    proto.String(upload.DirectPath),
			MediaKey:      upload.MediaKey,
			FileEncSHA256: upload.FileEncSHA256,
			FileSHA256:    upload.FileSHA256,
			FileLength:    proto.Uint64(upload.FileLength),
		}}
	case whatsmeow.MediaAudio:
		return &waProto.Message{AudioMessage: &waProto.AudioMessage{
			Mimetype:      proto.String(mime),
			URL:           proto.String(upload.URL),
			DirectPath:    proto.String(upload.DirectPath),
			MediaKey:      upload.MediaKey,
			FileEncSHA256: upload.FileEncSHA256,
			FileSHA256:    upload.FileSHA256,
			FileLength:    proto.Uint64(upload.FileLength),
			PTT:           proto.Bool(false),
		}}
	case whatsmeow.MediaImage:
		return &waProto.Message{ImageMessage: &waProto.ImageMessage{
			Caption:       proto.String(caption),
			Mimetype:      proto.String(mime),
			URL:           proto.String(upload.URL),
			DirectPath:    proto.String(upload.DirectPath),
			MediaKey:      upload.MediaKey,
			FileEncSHA256: upload.FileEncSHA256,
			FileSHA256:    upload.FileSHA256,
			FileLength:    proto.Uint64(upload.FileLength),
		}}
	default:
		return &waProto.Message{DocumentMessage: &waProto.DocumentMessage{
			Title:         proto.String(fileName),
			FileName:      proto.String(fileName),
			Mimetype:      proto.String(mime),
			URL:           proto.String(upload.URL),
			DirectPath:    proto.String(upload.DirectPath),
			MediaKey:      upload.MediaKey,
			FileEncSHA256: upload.FileEncSHA256,
			FileSHA256:    upload.FileSHA256,
			FileLength:    proto.Uint64(upload.FileLength),
		}}
	}
}

func (b *Bridge) emitState(state, detail string) {
	if b.listener != nil {
		b.listener.OnState(state, detail)
	}
}

func (b *Bridge) emitError(err error) {
	if b.listener != nil && err != nil {
		b.listener.OnError(err.Error())
	}
}

func onlyDigits(value string) string {
	var builder strings.Builder
	for _, char := range value {
		if char >= '0' && char <= '9' {
			builder.WriteRune(char)
		}
	}
	return builder.String()
}

func mimeFromPath(path string) string {
	switch strings.ToLower(filepath.Ext(path)) {
	case ".mp4":
		return "video/mp4"
	case ".mov":
		return "video/quicktime"
	case ".mkv":
		return "video/x-matroska"
	case ".m4a":
		return "audio/mp4"
	case ".mp3":
		return "audio/mpeg"
	case ".ogg", ".opus":
		return "audio/ogg"
	case ".jpg", ".jpeg":
		return "image/jpeg"
	case ".png":
		return "image/png"
	default:
		return "application/octet-stream"
	}
}

func (b *Bridge) cleanupOldMediaCache() {
	cacheDir := filepath.Join(b.rootDir, "media-cache")
	entries, err := os.ReadDir(cacheDir)
	if err != nil {
		return
	}
	cutoff := time.Now().Add(-24 * time.Hour)
	for _, entry := range entries {
		info, statErr := entry.Info()
		if statErr == nil && info.ModTime().Before(cutoff) {
			_ = os.Remove(filepath.Join(cacheDir, entry.Name()))
		}
	}
}

func safeMediaFileName(id, mime string) string {
	ext := ".jpg"
	switch strings.ToLower(mime) {
	case "image/png":
		ext = ".png"
	case "image/webp":
		ext = ".webp"
	case "image/gif":
		ext = ".gif"
	}
	var builder strings.Builder
	for _, char := range id {
		if (char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z') || (char >= '0' && char <= '9') || char == '-' || char == '_' {
			builder.WriteRune(char)
		}
	}
	if builder.Len() == 0 {
		builder.WriteString(fmt.Sprintf("%d", time.Now().UnixNano()))
	}
	return builder.String() + ext
}
