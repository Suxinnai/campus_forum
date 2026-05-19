import "./index.css";
import { Composition } from "remotion";
import { CampusForumDemo, VIDEO_DURATION_IN_FRAMES, VIDEO_FPS } from "./Composition";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="CampusForumDemo"
        component={CampusForumDemo}
        durationInFrames={VIDEO_DURATION_IN_FRAMES}
        fps={VIDEO_FPS}
        width={1920}
        height={1080}
      />
    </>
  );
};
