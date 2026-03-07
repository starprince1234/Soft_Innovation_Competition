class ApplicationError(Exception):
    """所有项目级异常的基类"""

    def __init__(self, message: str, code: int = 500):
        super().__init__(message)
        self.message = message
        self.code = code


class BizError(ApplicationError):
    """业务可预期错误（参数不合法、资源不存在等）"""

    def __init__(self, message: str, code: int = 400):
        super().__init__(message, code)


class AuthError(ApplicationError):
    """鉴权错误（token 无效 / IP 不在白名单）"""

    def __init__(self, message: str, code: int = 401):
        super().__init__(message, code)


class DependencyError(ApplicationError):
    """外部依赖不可用（LLM / 向量库 / 缓存 / 存储超时或异常）"""

    def __init__(self, message: str, code: int = 500):
        super().__init__(message, code)
