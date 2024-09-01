import asyncio

async def heartbeat():
    while True:
        await asyncio.sleep(10)
        yield ":\n\n"
