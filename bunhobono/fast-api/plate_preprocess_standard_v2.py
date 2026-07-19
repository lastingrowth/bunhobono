# plate_preprocess_standard_v2.py

from pathlib import Path
import hashlib
import tempfile

import cv2
import numpy as np


def imread_unicode(path):
    path = str(path)
    data = np.fromfile(path, dtype=np.uint8)
    if data.size == 0:
        return None
    return cv2.imdecode(data, cv2.IMREAD_COLOR)


def imwrite_unicode(path, image):
    path = str(path)
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    ext = Path(path).suffix or ".jpg"
    ok, buf = cv2.imencode(ext, image)
    if not ok:
        return False
    buf.tofile(path)
    return True


def clean_gray(image):
    if image is None:
        raise ValueError("image is None")
    if len(image.shape) == 2:
        return image
    return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)


def resize_height(image, target_h=96):
    h, w = image.shape[:2]
    if h <= 0 or w <= 0:
        return image
    ratio = target_h / h
    target_w = max(32, int(w * ratio))
    return cv2.resize(image, (target_w, target_h), interpolation=cv2.INTER_CUBIC)


def inner_crop(image, percent=3):
    h, w = image.shape[:2]
    px = int(w * percent / 100)
    py = int(h * percent / 100)
    if px <= 0 and py <= 0:
        return image
    return image[py:h - py, px:w - px]


def normalize_uint8(gray):
    lo, hi = np.percentile(gray, [2, 98])
    if hi - lo < 5:
        return gray.copy()
    out = (gray.astype(np.float32) - lo) * 255.0 / (hi - lo)
    return np.clip(out, 0, 255).astype(np.uint8)


def estimate_text_mask(gray):
    blur = cv2.GaussianBlur(gray, (3, 3), 0)
    try:
        _, otsu = cv2.threshold(
            blur, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU
        )
    except Exception:
        otsu = cv2.threshold(blur, 127, 255, cv2.THRESH_BINARY)[1]

    dark_mask = otsu == 0
    light_mask = otsu == 255

    dark_ratio = float(np.mean(dark_mask))
    light_ratio = float(np.mean(light_mask))

    # Korean plates usually have dark text on brighter background.
    # If Otsu chooses too much as text, fallback to percentile-based dark pixels.
    if 0.08 <= dark_ratio <= 0.45:
        mask = dark_mask
    elif 0.08 <= light_ratio <= 0.45 and np.mean(gray[light_mask]) < np.mean(gray[dark_mask]):
        mask = light_mask
    else:
        threshold = np.percentile(gray, 32)
        mask = gray <= threshold

    return mask.astype(np.uint8) * 255


def component_count(mask):
    num, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    count = 0
    h, w = mask.shape[:2]
    area_total = h * w

    for i in range(1, num):
        x, y, bw, bh, area = stats[i]
        if area < area_total * 0.002:
            continue
        if area > area_total * 0.25:
            continue
        if bh < h * 0.15:
            continue
        count += 1

    return count


def analyze_crop(image):
    gray = clean_gray(image)
    gray96 = resize_height(gray, 96)

    mean = float(np.mean(gray96))
    std = float(np.std(gray96))
    blur_score = float(cv2.Laplacian(gray96, cv2.CV_64F).var())

    p10, p50, p90 = np.percentile(gray96, [10, 50, 90])
    contrast_gap = float(p90 - p10)

    highlight_ratio = float(np.mean(gray96 > 225))
    shadow_ratio = float(np.mean(gray96 < 40))

    mask = estimate_text_mask(gray96)
    text_ratio = float(np.mean(mask > 0))
    comp_count = int(component_count(mask))

    bg_pixels = gray96[mask == 0]
    text_pixels = gray96[mask > 0]

    bg_mean = float(np.mean(bg_pixels)) if bg_pixels.size else mean
    text_mean = float(np.mean(text_pixels)) if text_pixels.size else mean
    bg_std = float(np.std(bg_pixels)) if bg_pixels.size else std

    tags = []

    if mean < 65:
        tags.append("dark")
    if mean > 160:
        tags.append("bright")
    if std < 22:
        tags.append("flat_low_std")
    if contrast_gap < 55:
        tags.append("low_contrast")
    if blur_score < 35:
        tags.append("very_blurry")
    elif blur_score < 90:
        tags.append("blurry")
    if highlight_ratio > 0.035:
        tags.append("glare")
    if shadow_ratio > 0.25:
        tags.append("shadow")
    if bg_std > 18:
        tags.append("uneven_background")
    if text_ratio < 0.10:
        tags.append("text_missing")
    if text_ratio > 0.42:
        tags.append("noise_heavy")
    if not tags:
        tags.append("normal")

    return {
        "mean": mean,
        "std": std,
        "blur_score": blur_score,
        "contrast_gap": contrast_gap,
        "highlight_ratio": highlight_ratio,
        "shadow_ratio": shadow_ratio,
        "text_ratio": text_ratio,
        "component_count": comp_count,
        "bg_mean": bg_mean,
        "text_mean": text_mean,
        "bg_std": bg_std,
        "tags": tags,
    }


def gray_candidate(image, height=96):
    gray = clean_gray(image)
    gray = resize_height(gray, height)
    return cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)


def standard_soft(image, height=96):
    gray = clean_gray(image)
    gray = inner_crop(gray, 2)
    gray = resize_height(gray, height)
    gray = normalize_uint8(gray)

    clahe = cv2.createCLAHE(clipLimit=1.8, tileGridSize=(8, 8))
    gray = clahe.apply(gray)

    blur = cv2.GaussianBlur(gray, (0, 0), 1.0)
    sharp = cv2.addWeighted(gray, 1.35, blur, -0.35, 0)

    return cv2.cvtColor(sharp, cv2.COLOR_GRAY2BGR)


def illumination_normalize(image, height=96, kernel=45):
    gray = clean_gray(image)
    gray = inner_crop(gray, 2)
    gray = resize_height(gray, height)

    if kernel % 2 == 0:
        kernel += 1

    bg = cv2.GaussianBlur(gray, (kernel, kernel), 0)
    norm = cv2.divide(gray, bg, scale=137)
    norm = normalize_uint8(norm)

    clahe = cv2.createCLAHE(clipLimit=1.6, tileGridSize=(8, 8))
    norm = clahe.apply(norm)

    return cv2.cvtColor(norm, cv2.COLOR_GRAY2BGR)


def local_binary_standard(image, height=96, block=31, c=7):
    gray = clean_gray(image)
    gray = inner_crop(gray, 2)
    gray = resize_height(gray, height)

    gray = cv2.medianBlur(gray, 3)
    gray = illumination_normalize(gray, height=height, kernel=45)
    gray = clean_gray(gray)

    if block % 2 == 0:
        block += 1

    bin_img = cv2.adaptiveThreshold(
        gray,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        block,
        c,
    )

    # black text on white background
    if np.mean(bin_img) < 127:
        bin_img = 255 - bin_img

    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (2, 2))
    bin_img = cv2.morphologyEx(bin_img, cv2.MORPH_OPEN, kernel, iterations=1)

    return cv2.cvtColor(bin_img, cv2.COLOR_GRAY2BGR)


def shadow_standard(image, height=112):
    gray = clean_gray(image)
    gray = inner_crop(gray, 2)
    gray = resize_height(gray, height)

    bg = cv2.GaussianBlur(gray, (61, 61), 0)
    corrected = cv2.divide(gray, bg, scale=150)
    corrected = normalize_uint8(corrected)

    clahe = cv2.createCLAHE(clipLimit=2.2, tileGridSize=(8, 8))
    corrected = clahe.apply(corrected)

    return cv2.cvtColor(corrected, cv2.COLOR_GRAY2BGR)


def glare_standard(image, height=96):
    gray = clean_gray(image)
    gray = inner_crop(gray, 2)
    gray = resize_height(gray, height)

    clipped = gray.copy()
    high = clipped > 220
    if np.any(high):
        median = np.median(clipped[~high]) if np.any(~high) else 180
        clipped[high] = median

    clipped = normalize_uint8(clipped)
    clipped = cv2.bilateralFilter(clipped, 5, 45, 45)

    return cv2.cvtColor(clipped, cv2.COLOR_GRAY2BGR)


def validate_candidate(image):
    gray = clean_gray(image)
    mask = estimate_text_mask(gray)

    text_ratio = float(np.mean(mask > 0))
    comp = component_count(mask)
    mean = float(np.mean(gray))
    std = float(np.std(gray))

    if text_ratio < 0.045:
        return False
    if text_ratio > 0.55:
        return False
    if std < 8:
        return False
    if comp < 1:
        return False
    if mean < 10 or mean > 248:
        return False

    return True


def add_candidate(candidates, mode, image, strict=True):
    if image is None:
        return
    if strict and not validate_candidate(image):
        return

    candidates.append({
        "mode": mode,
        "image": image,
    })


def make_candidates(image_path, output_dir=None, prefix=None):
    image = imread_unicode(image_path)
    if image is None:
        raise RuntimeError(f"이미지를 읽을 수 없습니다: {image_path}")

    metrics = analyze_crop(image)
    tags = set(metrics["tags"])

    candidates = []

    # Always keep raw and gray. These are the safest candidates.
    add_candidate(candidates, "raw", image.copy(), strict=False)
    add_candidate(candidates, "gray_h96", gray_candidate(image, 96), strict=False)
    add_candidate(candidates, "gray_h112", gray_candidate(image, 112), strict=False)

    # Soft standard candidates. These should not destroy the original structure.
    add_candidate(candidates, "standard_soft_h96", standard_soft(image, 96))
    add_candidate(candidates, "standard_soft_h112", standard_soft(image, 112))

    # State-based candidates.
    if {"dark", "shadow", "uneven_background", "flat_low_std"} & tags:
        add_candidate(candidates, "illum_norm_h96", illumination_normalize(image, 96, 45))
        add_candidate(candidates, "illum_norm_h112", illumination_normalize(image, 112, 55))
        add_candidate(candidates, "shadow_standard_h112", shadow_standard(image, 112))

    if {"glare", "bright"} & tags:
        add_candidate(candidates, "glare_standard_h96", glare_standard(image, 96))
        add_candidate(candidates, "glare_standard_h112", glare_standard(image, 112))

    if {"low_contrast", "flat_low_std", "very_blurry", "blurry"} & tags:
        add_candidate(candidates, "local_binary_h96", local_binary_standard(image, 96, 31, 7))
        add_candidate(candidates, "local_binary_h112", local_binary_standard(image, 112, 35, 7))

    # If image is very hard, add one stronger but still validated candidate.
    if len(tags - {"normal"}) >= 3:
        add_candidate(candidates, "hard_local_binary_h128", local_binary_standard(image, 128, 41, 8))

    if output_dir is not None:
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)

        if prefix is None:
            stem = Path(image_path).stem
            digest = hashlib.md5(str(image_path).encode("utf-8")).hexdigest()[:8]
            prefix = f"{stem}_{digest}"

        saved = []
        for i, cand in enumerate(candidates):
            out_path = output_dir / f"{prefix}_{i:02d}_{cand['mode']}.jpg"
            imwrite_unicode(out_path, cand["image"])
            saved.append({
                "mode": cand["mode"],
                "path": str(out_path),
                "metrics": metrics,
            })
        return saved

    return {
        "metrics": metrics,
        "candidates": candidates,
    }


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("image", help="crop image path")
    parser.add_argument("--out", default=None, help="output dir")
    args = parser.parse_args()

    out_dir = args.out
    if out_dir is None:
        out_dir = Path(tempfile.gettempdir()) / "plate_preprocess_standard_v2"

    result = make_candidates(args.image, output_dir=out_dir)

    print("candidate count:", len(result))
    for item in result:
        print(item["mode"], item["path"])