package storage

type Storage struct {
	DB    *Postgres
	Redis *Redis
	Minio *Minio
}
