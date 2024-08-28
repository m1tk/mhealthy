import asyncpg
import os

async def connect_to_db():
    dburl = os.getenv("DATABASE_URL")
    if dburl is None:
        raise Exception("DATABASE_URL env var is not present")
    return await asyncpg.pool.create_pool(dburl)

async def close_db_connection(pool):
    await pool.close()
