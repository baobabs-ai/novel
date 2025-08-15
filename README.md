# Light Novel Machine Translation Robot

[![GPL-3.0](https://img.shields.io/github/license/auto-novel/auto-novel)](https://github.com/auto-novel/auto-novel#license)
[![CI-Server](https://github.com/auto-novel/auto-novel/workflows/CI-Server/badge.svg)](https://github.com/auto-novel/auto-novel/actions/workflows/CI-Server.yml)
[![CI-Web](https://github.com/auto-novel/auto-novel/workflows/CI-Web/badge.svg)](https://github.com/auto-novel/auto-novel/actions/workflows/CI-Web.yml)

> Rebuilding the Tower of Babel!!

[Light Novel Machine Translation Robot](https://books.fishhawk.top/) is a website that automatically generates and shares machine-translated light novels. Here, you can browse Japanese web novels/library novels, or upload your own EPUB/TXT files to generate machine-translated versions.

## Features

- Browse Japanese web novels, supported sites include: [Kakuyomu](https://kakuyomu.jp/), [Shōsetsuka ni Narō](https://syosetu.com/), [Novelup](https://novelup.plus/), [Hameln](https://syosetu.org/), [Pixiv](https://www.pixiv.net/), [Alphapolis](https://www.alphapolis.co.jp/).
- Generate multiple machine translations, supported translators include: Baidu, Youdao, OpenAI-like API (e.g., DeepSeek API), [Sakura](https://huggingface.co/SakuraLLM/Sakura-14B-Qwen2.5-v1.0-GGUF).
- Supports glossaries.
- Supports multiple formats, including Japanese, Chinese, and Chinese-Japanese comparison.
- Supports generating EPUB and TXT files.
- Supports translating EPUB and TXT files.
- Supports online reading.

## Contribution

Please refer to [CONTRIBUTING.md](https://github.com/auto-novel/auto-novel/blob/main/CONTRIBUTING.md)

<a href="https://next.ossinsight.io/widgets/official/compose-recent-top-contributors?repo_id=559577341" target="_blank" style="display: block" align="left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://next.ossinsight.io/widgets/official/compose-recent-top-contributors/thumbnail.png?repo_id=559577341&image_size=auto&color_scheme=dark" width="280">
    <img alt="Top Contributors of ant-design/ant-design - Last 28 days" src="https://next.ossinsight.io/widgets/official/compose-recent-top-contributors/thumbnail.png?repo_id=559577341&image_size=auto&color_scheme=light" width="280">
  </picture>
</a>

## Deployment

> [!WARNING]
> Note: This project is not designed for personal deployment. Not all features are guaranteed to be available or forward-compatible.

Download the project:

```bash
> git clone https://github.com/auto-novel/auto-novel.git
> cd auto-novel
```

Create and edit the `.env` file with the following content:

```bash
DATA_PATH=./data                      # Data storage location
HTTPS_PROXY=https://127.0.0.1:7890    # Proxy for web novels, can be empty
PIXIV_COOKIE_PHPSESSID=               # Pixiv cookies, not required if you don't use Pixiv
```

Open the `docker-compose.yml` file and modify it as needed.

Run `docker compose up [-d]` (`-d` for background execution).

Access `http://localhost` to get started.
