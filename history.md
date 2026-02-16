
# History

## 2026-02-16 - Version 1.1.2
- Updated strings.xml and minor UI text improvements.
- Merged latest changes from remote and resolved all merge conflicts.
- Updated documentation and release notes for v1.1.2.
- Tagged and released as v1.1.2.

## 2026-02-15 - Version 1.1.1
- Updated app name to include version number (Trigger v1.1.1).
- Refactored trigger handling to route events through handleTriggerPress method.
- Improved tag clearing behavior by adding clearTagData calls before scanning and in response handler.
- Removed unused testFunction method from MainActivity.
- Added VS Code build tasks configuration for streamlined development.
- Successfully built and deployed app to device 59040DLCH003LK.

## 2026-02-10
- Build and run process completed successfully on macOS.
- Fixed Android SDK location error by creating local.properties.
- Removed android:onClick from XML to resolve lint errors.
- Increased minSdkVersion to 28 to resolve API compatibility issues.
- Application installed and launched on device using build_deploy_launch.sh.

## 2026-02-10 (post-release)
- Major code cleanup and refactoring for maintainability and style compliance.
- Reduced method complexity and improved naming conventions in all main Java classes.
- Removed unused fields, improved exception handling, and modernized code style.
- Ready for commit and further development.
