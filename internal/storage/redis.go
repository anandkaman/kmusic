package storage

import (
	"context"
	"fmt"

	"github.com/redis/go-redis/v9"
	"musicstream/internal/config"
)

type Redis struct {
	Client *redis.Client
}

func NewRedis(cfg config.RedisConfig) *Redis {
	client := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.Host, cfg.Port),
		Password: "",
		DB:       0,
		PoolSize: 10,
	})

	return &Redis{Client: client}
}

func (r *Redis) Close() error {
	return r.Client.Close()
}

func (r *Redis) Ping(ctx context.Context) error {
	return r.Client.Ping(ctx).Err()
}
