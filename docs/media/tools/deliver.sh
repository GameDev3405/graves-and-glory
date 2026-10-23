#!/bin/bash
# usage: deliver.sh  -- builds work/media/delivery from the 1440p masters in work/media/clips
FF="C:/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg.exe"
M="C:/Users/kortt/Melty/bannerbound/work/media"
mkdir -p "$M/delivery/clips-1080p60" "$M/delivery/reels-9x16"
enc() {
  f="$1"; n=$(basename "$f" .mp4)
  "$FF" -y -hide_banner -loglevel error -i "$f" -an \
    -vf "scale=1920:1080:flags=lanczos,format=yuv420p" \
    -c:v libx264 -preset medium -crf 14 -profile:v high -r 60 \
    -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv -movflags +faststart \
    "$M/delivery/clips-1080p60/$n.mp4"
  "$FF" -y -hide_banner -loglevel error -i "$f" -an \
    -vf "crop=810:1440:(iw-810)/2:0,scale=1080:1920:flags=lanczos,format=yuv420p" \
    -c:v libx264 -preset medium -crf 15 -profile:v high -r 60 \
    -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv -movflags +faststart \
    "$M/delivery/reels-9x16/$n.mp4"
  echo "done $n"
}
export -f enc; export FF M
ls "$M"/clips/{kingsfield,ironhold,rivergate,hollowwood,frostmarch}-c?-*.mp4 | xargs -P 4 -I{} bash -c 'enc {}'
