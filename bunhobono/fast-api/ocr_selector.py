# ocr_selector.py

import re
from collections import Counter


REGION_NAMES = (
    "서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주"
)

CURRENT_PLATE_RE = re.compile(r"^\d{2,3}[가-힣]\d{4}$")
OLD_REGION_PLATE_RE = re.compile(
    rf"^({REGION_NAMES})\d{{1,2}}[가-힣]\d{{4}}$"
)


def clean_text(text: str) -> str:
    return re.sub(r"[^0-9가-힣]", "", str(text))


def is_valid_plate(text: str) -> bool:
    text = clean_text(text)

    return (
        CURRENT_PLATE_RE.fullmatch(text) is not None
        or OLD_REGION_PLATE_RE.fullmatch(text) is not None
    )


def split_plate(text: str):
    text = clean_text(text)

    current = re.fullmatch(r"(\d{2,3})([가-힣])(\d{4})", text)
    if current:
        return current.group(1), current.group(2), current.group(3)

    old_region = re.fullmatch(
        rf"({REGION_NAMES})(\d{{1,2}})([가-힣])(\d{{4}})",
        text,
    )
    if old_region:
        return (
            old_region.group(1) + old_region.group(2),
            old_region.group(3),
            old_region.group(4),
        )

    return "", "", ""


def number_count(text: str) -> int:
    return len(re.findall(r"\d", clean_text(text)))


def hangul_count(text: str) -> int:
    return len(re.findall(r"[가-힣]", clean_text(text)))


def has_region_name(text: str) -> bool:
    text = clean_text(text)
    return re.match(rf"^({REGION_NAMES})", text) is not None


def format_score(text: str) -> int:
    text = clean_text(text)

    if is_valid_plate(text):
        if has_region_name(text):
            return 105
        return 100

    score = 0

    if re.match(r"^\d{2,3}", text):
        score += 25

    if re.match(rf"^({REGION_NAMES})", text):
        score += 25

    if re.search(r"[가-힣]", text):
        score += 30

    if re.search(r"\d{4}$", text):
        score += 25

    if 7 <= len(text) <= 10:
        score += 15

    if re.fullmatch(r"[0-9가-힣]+", text):
        score += 5

    return score


def mode_bonus(mode: str) -> int:
    mode = str(mode)

    if mode.startswith("standard_soft"):
        return 28

    if mode.startswith("illum_norm"):
        return 22

    if mode.startswith("shadow_standard"):
        return 20

    if mode.startswith("gray"):
        return 8

    if mode == "raw":
        return 4

    if mode.startswith("glare_standard"):
        return 6

    if "local_binary" in mode:
        return -8

    return 0


def selector_v2_score(candidate: dict, repeat_count: int) -> float:
    pred = clean_text(candidate.get("text", candidate.get("pred", "")))
    ocr_score = float(candidate.get("score", 0.0))
    mode = str(candidate.get("mode", ""))

    score = 0.0

    score += format_score(pred) * 4
    score += min(ocr_score, 1.0) * 120

    # 반복은 참고하되, 반복 오답에 끌려가지 않도록 상한을 둔다.
    score += min(repeat_count, 3) * 6

    score += mode_bonus(mode)

    if is_valid_plate(pred):
        score += 60

    # 신형: 7~8자, 구형 지역명 포함: 보통 8~10자까지 허용
    if not (7 <= len(pred) <= 10):
        score -= 80

    if not has_region_name(pred) and hangul_count(pred) != 1:
        score -= 70

    if has_region_name(pred) and hangul_count(pred) < 3:
        # 서울, 부산 같은 지역명 + 용도 한글 1자가 있어야 하므로 보통 한글 3자 이상.
        score -= 35

    nums = number_count(pred)
    if nums not in [5, 6, 7]:
        score -= 40

    if len(pred) < 6:
        score -= 120

    if pred == "":
        score -= 200

    return score


def select_best_ocr(candidates: list[dict]) -> dict:
    if not candidates:
        return {
            "text": "",
            "raw_text": "",
            "score": 0.0,
            "mode": "no_candidate",
            "selector_score": 0.0,
            "repeat_count": 0,
            "is_valid_plate": False,
            "all_candidates": [],
        }

    normalized = []

    for c in candidates:
        text = clean_text(c.get("text", c.get("pred", "")))

        item = dict(c)
        item["text"] = text
        item["score"] = float(c.get("score", 0.0))
        item["mode"] = str(c.get("mode", ""))
        item["raw_text"] = str(c.get("raw_text", text))

        normalized.append(item)

    repeat_counter = Counter(
        c["text"] for c in normalized if c["text"]
    )

    best = None
    best_score = -999999.0

    for c in normalized:
        repeat_count = repeat_counter.get(c["text"], 0)
        s = selector_v2_score(c, repeat_count)

        c["selector_score"] = float(s)
        c["repeat_count"] = int(repeat_count)
        c["is_valid_plate"] = is_valid_plate(c["text"])

        if s > best_score:
            best_score = s
            best = c

    if best is None:
        return {
            "text": "",
            "raw_text": "",
            "score": 0.0,
            "mode": "no_candidate",
            "selector_score": 0.0,
            "repeat_count": 0,
            "is_valid_plate": False,
            "all_candidates": normalized,
        }

    return {
        "text": best["text"],
        "raw_text": best.get("raw_text", best["text"]),
        "score": float(best.get("score", 0.0)),
        "mode": best.get("mode", ""),
        "selector_score": float(best.get("selector_score", 0.0)),
        "repeat_count": int(best.get("repeat_count", 0)),
        "is_valid_plate": bool(best.get("is_valid_plate", False)),
        "image_path": best.get("image_path", ""),
        "all_candidates": normalized,
    }


def should_accept_single_frame(result: dict) -> bool:
    """
    단일 이미지에서 바로 확정 가능한지 판단.
    실서비스에서는 이 값이 False면 다중프레임/재촬영/오류 처리로 넘기는 게 안전하다.
    """
    text = clean_text(result.get("text", ""))
    score = float(result.get("score", 0.0))
    repeat_count = int(result.get("repeat_count", 0))
    valid = is_valid_plate(text)

    if not valid:
        return False

    if score >= 0.995 and repeat_count >= 2:
        return True

    if score >= 0.998 and result.get("mode") in ["raw", "gray_h96", "gray_h112"]:
        return True

    return False