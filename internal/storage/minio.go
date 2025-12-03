package storage

import (
	"context"
	"fmt"
	"io"

	"github.com/minio/minio-go/v7"
	"github.com/minio/minio-go/v7/pkg/credentials"
	"musicstream/internal/config"
)

type Minio struct {
	Client *minio.Client
	Bucket string
}

func NewMinio(cfg config.MinioConfig) (*Minio, error) {
	client, err := minio.New(cfg.Endpoint, &minio.Options{
		Creds:  credentials.NewStaticV4(cfg.AccessKey, cfg.SecretKey, ""),
		Secure: cfg.UseSSL,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to create minio client: %w", err)
	}

	// Create bucket if it doesn't exist
	ctx := context.Background()
	exists, err := client.BucketExists(ctx, cfg.Bucket)
	if err != nil {
		return nil, fmt.Errorf("failed to check bucket existence: %w", err)
	}

	if !exists {
		err = client.MakeBucket(ctx, cfg.Bucket, minio.MakeBucketOptions{})
		if err != nil {
			return nil, fmt.Errorf("failed to create bucket: %w", err)
		}
	}

	return &Minio{
		Client: client,
		Bucket: cfg.Bucket,
	}, nil
}

func (m *Minio) Upload(ctx context.Context, objectName string, reader io.Reader, size int64, contentType string) error {
	_, err := m.Client.PutObject(ctx, m.Bucket, objectName, reader, size, minio.PutObjectOptions{
		ContentType: contentType,
	})
	return err
}

func (m *Minio) Get(ctx context.Context, objectName string) (*minio.Object, error) {
	return m.Client.GetObject(ctx, m.Bucket, objectName, minio.GetObjectOptions{})
}

func (m *Minio) Delete(ctx context.Context, objectName string) error {
	return m.Client.RemoveObject(ctx, m.Bucket, objectName, minio.RemoveObjectOptions{})
}

func (m *Minio) GetInfo(ctx context.Context, objectName string) (minio.ObjectInfo, error) {
	return m.Client.StatObject(ctx, m.Bucket, objectName, minio.StatObjectOptions{})
}
