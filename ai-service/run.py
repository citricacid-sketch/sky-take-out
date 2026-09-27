"""ai-service 启动脚本。"""

import uvicorn

from app.config import settings

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="127.0.0.1", port=settings.ai_service_port, reload=True)
