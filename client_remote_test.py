from openai import OpenAI

# AutoDL 连接配置说明：
# 1. 在 AutoDL 实例列表点击 "自定义服务"
# 2. 找到对应 6006 端口的 "公网地址"
# 3. 将其填入下方 BASE_URL，并在末尾加上 /v1

# 示例: BASE_URL = "https://u817542-xxxx.bjb2.seetacloud.com:8443/v1"
# 请务必修改下面的地址！！！
BASE_URL = "https://u817542-83de-33e3ef6c.bjb2.seetacloud.com:8443/v1" 
API_KEY = "sk-123456" 

client = OpenAI(
    api_key=API_KEY,
    base_url=BASE_URL,
)

# 查看可用模型
try:
    print("Available models:")
    models = client.models.list()
    for model in models:
        print(f"- {model.id}")
except Exception as e:
    print(f"连接失败，请检查 BASE_URL 是否正确: {e}")
    exit(1)

print("\n--- Test 1: Pure Text ---")
try:
    completion = client.chat.completions.create(
        model="qwen3-vl-lora",
        messages=[
            {"role": "user", "content": "你好，请回复'收到'。"}
        ],
        temperature=0.7,
        max_tokens=100,
        stream=True
    )
    print("Response: ", end="")
    for chunk in completion:
        if chunk.choices[0].delta.content:
            print(chunk.choices[0].delta.content, end="", flush=True)
    print("\n")
except Exception as e:
    print(f"Text Test Failed: {e}")

import base64

# 读取本地图片并转换为 Base64
def encode_image(image_path):
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')

# 本地图片路径
LOCAL_IMAGE_PATH = "test.jpeg"

print(f"\n--- Test 3: Local Image ({LOCAL_IMAGE_PATH}) ---")
try:
    # 检查文件是否存在
    import os
    if not os.path.exists(LOCAL_IMAGE_PATH):
        print(f"Error: {LOCAL_IMAGE_PATH} not found!")
    else:
        print("Encoding image...")
        base64_image = encode_image(LOCAL_IMAGE_PATH)
        
        print("Sending chat request with BASE64 IMAGE...")
        completion = client.chat.completions.create(
            model="qwen3-vl-lora",
            messages=[
                {"role": "user", "content": [
                    {"type": "text", "text": "你是专业的考古学家，要认真确定这张图片讲的是什么著名文物，给我详细讲讲？"},
                    {"type": "image_url", "image_url": {
                        "url": f"data:image/jpeg;base64,{base64_image}"
                    }}
                ]}
            ],
            # 调整生成参数以避免乱码
            temperature=0.6,  # 降低随机性，使其更聚焦
            top_p=0.8,        # 限制采样范围
            max_tokens=2048,
            stream=True,
            # 显式指定停止符，防止模型停不下来导致崩坏
            stop=["<|im_end|>", "<|endoftext|>", "<|end|>"]
        )

        print("Response (Raw):")
        full_response = ""
        for chunk in completion:
            content = chunk.choices[0].delta.content
            if content:
                # 直接打印原始内容，不做任何处理，避免终端编码问题干扰判断
                # 如果是乱码，可能是因为 ANSI 颜色代码或者特殊 unicode
                try:
                    print(content, end="", flush=True)
                except UnicodeEncodeError:
                    # 遇到无法打印的字符，用 ? 代替
                    print(content.encode('utf-8', 'replace').decode('utf-8'), end="", flush=True)
                full_response += content
        print("\n\n--- End of Response ---")


except Exception as e:
    print(f"\nLocal Image Test Failed: {e}")
