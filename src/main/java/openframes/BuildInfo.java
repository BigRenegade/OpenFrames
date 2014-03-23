package openframes;

/**
 * This file is automatically updated by Jenkins as part of the CI build script
 * in Ant. Don't put any pre-set values here.
 * @author AfterLifeLochie/Caitlyn
 */
public class BuildInfo {
	public static final String modName = "OpenFrames";
	public static final String modID = "OpenFrames";

	public static final String versionNumber = "@VERSION@";
	public static final String buildNumber = "@BUILD@";

	public static int getBuildNumber() {
		if (buildNumber.equals("@" + "BUILD" + "@"))
			return 0;
		return Integer.parseInt(buildNumber);
	}
}
