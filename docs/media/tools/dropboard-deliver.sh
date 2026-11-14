#!/bin/bash
# usage: dropboard-deliver.sh <video.mp4> ...  For each video: a 9:16 centre crop (1080x1920, from the middle of the frame) and a 12-frame contact sheet (4x3 jpg),
# and one line of facts (resolution, fps, frames, length). The drop-board shoot, 2026-11-14.
FF=/c/ffmpeg/ffmpeg-8.1-full_build/bin
for v in "$@"; do
  b="${v%.mp4}"
  dur=$($FF/ffprobe -v error -show_entries format=duration -of csv=p=0 "$v")
  info=$($FF/ffprobe -v error -select_streams v:0 -count_frames -show_entries stream=width,height,r_frame_rate,nb_read_frames -of csv=p=0 "$v")
  echo "$(basename "$v"): $info, ${dur}s"
  w=$(echo "$info" | cut -d, -f1); h=$(echo "$info" | cut -d, -f2)
  cw=$(( h * 9 / 16 / 2 * 2 ))
  $FF/ffmpeg -y -v error -i "$v" -vf "crop=$cw:$h:(iw-$cw)/2:0,scale=1080:1920:flags=lanczos,format=yuv420p" -c:v libx264 -crf 15 -preset medium -movflags +faststart "${b}-9x16.mp4"
  rate=$(python -c "print(12/float('$dur'))")
  $FF/ffmpeg -y -v error -i "$v" -vf "fps=$rate,scale=640:-1,tile=4x3" -frames:v 1 -q:v 3 "${b}-sheet.jpg"
done
