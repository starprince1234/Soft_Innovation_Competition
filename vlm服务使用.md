# Qwen 文物考古学家 API

这个目录保存了已经部署好的 `Qwen3.5-35B-A3B-FP8` 文物考古学家模型的调用说明，以及一个本地可运行的测试脚本。

## 配置信息

- 模型仓库：`Qwen/Qwen3.5-35B-A3B-FP8`
- 对外模型名：`qwen3.5-35b-a3b-archaeology`
- `base_url`：通过环境变量 `VLM_BASE_URL` 配置，例如 `https://your-vlm-host.example.com/v1`
- 健康检查：由模型服务提供方决定，常见形式为 `https://your-vlm-host.example.com/health`
- `api_key`：通过环境变量 `VLM_API_KEY` 配置，不要写入仓库

## 模型能力设定

服务端已经注入了系统提示词，模型会默认以“文物与考古专家”的身份回答问题，重点输出：

1. 可能的文物名称
2. 年代或时期判断及依据
3. 器形、纹饰、工艺、材质特征
4. 可能用途与历史背景
5. 与相似文物的区别
6. 结论置信度

如果证据不足，模型会明确说明不确定，并提示需要补充哪些照片角度、尺寸、铭文或出土信息。

## 推荐调用方式

建议请求里带上：

```json
"chat_template_kwargs": {
  "enable_thinking": false
}
```

这样返回的是正常答案，不会把推理过程展开。

## curl 示例

```bash
curl "$VLM_BASE_URL/chat/completions" \
  -H "Authorization: Bearer $VLM_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen3.5-35b-a3b-archaeology",
    "messages": [
      {
        "role": "user",
        "content": "这件三足两耳的青铜器可能是什么？请简要介绍。"
      }
    ],
    "temperature": 0.2,
    "max_tokens": 300,
    "chat_template_kwargs": {
      "enable_thinking": false
    }
  }'
```

## 图片识别示例

```bash
curl "$VLM_BASE_URL/chat/completions" \
  -H "Authorization: Bearer $VLM_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen3.5-35b-a3b-archaeology",
    "messages": [
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": "请识别这件文物，并介绍年代、材质、用途和判断依据。"
          },
          {
            "type": "image_url",
            "image_url": {
              "url": "https://example.com/artifact.jpg"
            }
          }
        ]
      }
    ],
    "temperature": 0.2,
    "max_tokens": 500,
    "chat_template_kwargs": {
      "enable_thinking": false
    }
  }'
```

## 本地测试脚本

文件：`test_qwen_arch_api.py`

直接运行：

```bash
python3 test_qwen_arch_api.py
```

自定义提问：

```bash
python3 test_qwen_arch_api.py --prompt "请介绍青铜鼎的基本特征。"
```

带图片 URL：

```bash
python3 test_qwen_arch_api.py \
  --prompt "请识别这件文物并说明判断依据。" \
  --image-url "https://example.com/artifact.jpg"
```

## 可覆盖的环境变量

- `QWEN_ARCH_BASE_URL`
- `QWEN_ARCH_API_KEY`
- `QWEN_ARCH_MODEL`

当前仓库不保存真实模型地址或密钥。如果不设环境变量，脚本应直接提示缺少配置；生产值应配置在 `.env`、GitHub Secrets 或部署平台环境变量中。
