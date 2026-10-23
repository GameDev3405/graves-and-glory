#!/bin/bash
# usage: deliver2.sh <media dir>  -- builds delivery/ from the 1440p masters in <media dir>/clips
# the slow-motion master is captured at 120 fps and played at half speed (60 fps out); SLOWMO_SS trims its first seconds of source time
FF="C:/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg.exe"
M="$1"
mkdir -p "$M/delivery/clips-1080p60" "$M/delivery/reels-9x16" "$M/delivery/previews-720p"
enc() {
  f="$1"; n=$(basename "$f" .mp4)
  pre=""; ss=""
  if [[ "$n" == *slowmo* ]]; then pre="setpts=2.0*PTS,fps=60,"; ss="-ss ${SLOWMO_SS:-0}"; fi
  "$FF" -y -hide_banner -loglevel error $ss -i "$f" -an \
    -vf "${pre}scale=1920:1080:flags=lanczos,format=yuv420p" \
    -c:v libx264 -preset medium -crf 14 -profile:v high -r 60 \
    -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv -movflags +faststart \
    "$M/delivery/clips-1080p60/$n.mp4"
  "$FF" -y -hide_banner -loglevel error $ss -i "$f" -an \
    -vf "${pre}crop=810:1440:(iw-810)/2:0,scale=1080:1920:flags=lanczos,format=yuv420p" \
    -c:v libx264 -preset medium -crf 15 -profile:v high -r 60 \
    -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv -movflags +faststart \
    "$M/delivery/reels-9x16/$n.mp4"
  "$FF" -y -hide_banner -loglevel error -i "$M/delivery/clips-1080p60/$n.mp4" -an \
    -vf "scale=1280:720:flags=lanczos,format=yuv420p" -c:v libx264 -preset fast -crf 25 -r 60 -movflags +faststart \
    "$M/delivery/previews-720p/$n.mp4"
  echo "done $n"
}
export -f enc; export FF M
ls "$M"/clips/*-c*.mp4 | xargs -P 4 -I{} bash -c 'enc {}'
