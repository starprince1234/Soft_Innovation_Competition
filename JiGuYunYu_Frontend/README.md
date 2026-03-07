# JiGuYunYu_Frontend

最小联调页面，用于验证 Java / Python 后端接口。

## 运行方式

在本目录执行：

```bash
python -m http.server 5173
```

浏览器访问：

- http://localhost:5173/

> 如出现跨域限制，请优先通过 Nginx 入口（`http://localhost`）进行接口验证，或在后端临时放开本地联调域名。
