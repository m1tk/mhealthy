import asyncpg
import os
import asyncio
import asyncpg_listen

async def connect_to_db():
    dburl = os.getenv("DATABASE_URL")
    if dburl is None:
        raise Exception("DATABASE_URL env var is not present")
    return await asyncpg.pool.create_pool(dburl)

async def close_db_connection(pool):
    await pool.close()

async def event_listener(event_handler):
    listener = asyncpg_listen.NotificationListener(asyncpg_listen.connect_func(os.getenv("DATABASE_URL")))
    asyncio.create_task(
        listener.run(
            {"instruction": event_handler, "patient_info": event_handler},
            policy=asyncpg_listen.ListenPolicy.ALL
        )
    )

