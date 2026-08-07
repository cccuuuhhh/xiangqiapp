#!/usr/bin/env python3
"""
下载 Pikafish NNUE 权重文件脚本。

用法：
    python scripts/download_nnue.py

来源：
    - 优先从 Pikafish 官方 CDN 下载
    - 回退到 GitHub Releases

产出：
    app/src/main/assets/pikafish.nnue (~30 MB)
"""

import os
import sys
import urllib.request
import hashlib
from pathlib import Path

# 配置
PROJECT_ROOT = Path(__file__).resolve().parent.parent
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
NNUE_FILENAME = "pikafish.nnue"
OUTPUT_PATH = ASSETS_DIR / NNUE_FILENAME

# 下载源（按优先级）
SOURCES = [
    "http://test.pikafish.org/api/nnue/download/latest",
    "https://github.com/official-pikafish/Pikafish/releases/latest/download/pikafish.nnue",
    "https://raw.githubusercontent.com/official-pikafish/NNUE-Nets/main/pikafish.nnue",
]

EXPECTED_SHA256 = None  # 如果知道预期 hash 可在此填写
EXPECTED_SIZE_MIN = 20 * 1024 * 1024  # 至少 20 MB


def download_file(url: str, output: Path) -> bool:
    """下载文件，返回是否成功。"""
    print(f"[DOWNLOAD] {url} → {output}")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "xiangqiapp-downloader/1.0"})
        with urllib.request.urlopen(req, timeout=120) as resp:
            content = resp.read()

        if len(content) < EXPECTED_SIZE_MIN:
            print(f"[WARN] 文件过小 ({len(content)} bytes)，可能不完整，跳过")
            return False

        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(content)

        sha = hashlib.sha256(content).hexdigest()
        print(f"[OK] 下载完成: {len(content)} bytes, SHA256={sha[:16]}...")
        return True

    except Exception as e:
        print(f"[FAIL] {e}")
        return False


def main():
    # 创建 assets 目录
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)

    # 如果已存在，检查大小
    if OUTPUT_PATH.exists():
        size = OUTPUT_PATH.stat().st_size
        if size >= EXPECTED_SIZE_MIN:
            print(f"[SKIP] {OUTPUT_PATH} 已存在 ({size} bytes)，跳过下载")
            print("       如需重新下载，请先手动删除该文件")
            return 0
        else:
            print(f"[WARN] 已存在的文件过小 ({size} bytes)，将重新下载")

    # 依次尝试下载源
    for i, url in enumerate(SOURCES):
        print(f"\n--- 尝试 #{i + 1}: {url} ---")
        if download_file(url, OUTPUT_PATH):
            # 验证
            size = OUTPUT_PATH.stat().st_size
            if size < EXPECTED_SIZE_MIN:
                print(f"[WARN] 下载文件大小异常 ({size} bytes)")
                OUTPUT_PATH.unlink(missing_ok=True)
                continue

            if EXPECTED_SHA256:
                sha = hashlib.sha256(OUTPUT_PATH.read_bytes()).hexdigest()
                if sha != EXPECTED_SHA256:
                    print(f"[WARN] SHA256 不匹配: {sha}")
                    OUTPUT_PATH.unlink(missing_ok=True)
                    continue

            print(f"\n✅ NNUE 权重文件已就绪: {OUTPUT_PATH}")
            return 0

    print("\n❌ 所有下载源均失败。请手动下载 pikafish.nnue 并放入 assets/ 目录")
    print("   下载地址: https://github.com/official-pikafish/Pikafish/releases")
    return 1


if __name__ == "__main__":
    sys.exit(main())
