import os
from PIL import Image, ImageDraw

def generate_all_icons(src_path: str = "logo_source.png", res_base: str = "app/src/main/res"):
    if not os.path.exists(src_path):
        print(f"Source image not found at {src_path}. Skipping icon generation.")
        return

    src_img = Image.open(src_path).convert("RGBA")
    w, h = src_img.size

    # 1. Create transparent squircle from the source image
    # Squircle bounding box is (24, 10, 1000, 1010)
    crop_box = (24, 10, 1000, 1010)
    cropped_src = src_img.crop(crop_box)
    cw, ch = cropped_src.size
    
    # Make a square cropped image (approx 976 x 1000 -> 1000 x 1000)
    size = max(cw, ch)
    square_src = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset_x = (size - cw) // 2
    offset_y = (size - ch) // 2
    square_src.paste(cropped_src, (offset_x, offset_y))

    # Mask with rounded corners (radius ~22% of dimension)
    squircle_mask = Image.new("L", (size, size), 0)
    draw_sq = ImageDraw.Draw(squircle_mask)
    draw_sq.rounded_rectangle([(0, 0), (size, size)], radius=int(size * 0.22), fill=255)

    base_squircle_logo = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base_squircle_logo.paste(square_src, (0, 0), mask=squircle_mask)

    # 2. Create circular masked logo
    circle_mask = Image.new("L", (size, size), 0)
    draw_cir = ImageDraw.Draw(circle_mask)
    draw_cir.ellipse([(0, 0), (size, size)], fill=255)

    base_circle_logo = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base_circle_logo.paste(square_src, (0, 0), mask=circle_mask)

    # 3. Create high-res in-app logo (drawable/ic_kharchaflow_logo.png)
    drawable_dir = os.path.join(res_base, "drawable")
    os.makedirs(drawable_dir, exist_ok=True)
    high_res_logo = base_squircle_logo.resize((512, 512), Image.Resampling.LANCZOS)
    high_res_logo.save(os.path.join(drawable_dir, "ic_kharchaflow_logo.png"), "PNG")
    print("Saved 512x512 ic_kharchaflow_logo.png")

    # 4. Standard and Round launcher icon densities
    # mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
    densities = {
        "mipmap-mdpi": (48, 108),
        "mipmap-hdpi": (72, 162),
        "mipmap-xhdpi": (96, 216),
        "mipmap-xxhdpi": (144, 324),
        "mipmap-xxxhdpi": (192, 432)
    }

    for folder_name, (icon_size, adaptive_size) in densities.items():
        folder_path = os.path.join(res_base, folder_name)
        os.makedirs(folder_path, exist_ok=True)

        # Legacy squircle icon
        icon_img = base_squircle_logo.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        icon_img.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")

        # Round icon
        round_img = base_circle_logo.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        round_img.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")

        # Adaptive icon foreground (108dp canvas, safe zone 72dp -> 66.7% scaling)
        fg_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
        # Scale the squircle logo to fit comfortably inside the safe circle (approx 70% of canvas)
        inner_logo_size = int(adaptive_size * 0.70)
        inner_logo = base_squircle_logo.resize((inner_logo_size, inner_logo_size), Image.Resampling.LANCZOS)
        fg_offset = (adaptive_size - inner_logo_size) // 2
        fg_canvas.paste(inner_logo, (fg_offset, fg_offset), mask=inner_logo)
        fg_canvas.save(os.path.join(folder_path, "ic_launcher_foreground.png"), "PNG")

        print(f"Generated {folder_name}: ic_launcher ({icon_size}x{icon_size}), ic_launcher_round ({icon_size}x{icon_size}), ic_launcher_foreground ({adaptive_size}x{adaptive_size})")

    print("\nAll Leaf launcher and branding icon assets generated successfully!")

if __name__ == "__main__":
    generate_all_icons()
