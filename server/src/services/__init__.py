import asyncio

async def heartbeat():
    while True:
        await asyncio.sleep(30)
        yield ":\n\n"


async def heartbeat_inter(stop: asyncio.Event):
    while True:
        sleep = asyncio.sleep(30)
        done, pending = await asyncio.wait(
                {sleep, stop.wait()},
                return_when=asyncio.FIRST_COMPLETED
            )
        for task in pending:
            task.cancel()
        if stop.is_set():
            return
        yield ":\n\n"
