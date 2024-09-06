import asyncio

async def heartbeat():
    while True:
        await asyncio.sleep(30)
        yield ":\n\n"
