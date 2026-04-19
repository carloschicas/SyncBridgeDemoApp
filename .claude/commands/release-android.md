Triggers the GitHub Actions release workflow for the Android (Kotlin) app. Required argument: version in format v1.0.0

The received version is: $ARGUMENTS

Execute the following steps in order:

1. Validate that $ARGUMENTS is not empty and matches exactly the format `vMAJOR.MINOR.PATCH` (e.g. v1.0.0, v2.3.14).
   - Valid format: a lowercase "v" followed by three dot-separated numbers.
   - If $ARGUMENTS is empty or does not match the format, stop and show:
     ```
     ❌ Invalid argument: "$ARGUMENTS"

     Required format: v<MAJOR>.<MINOR>.<PATCH>
     Example: /release-android v0.1.0

     Existing Android tags:
     ```
     Then run `git tag --list "android/*" --sort=-version:refname` and list them. If none exist, show "No tags published yet".

2. Build the tag name: `android/$ARGUMENTS`
   Example: if $ARGUMENTS is "v0.2.0", the tag is "android/v0.2.0"

3. Check if the tag already exists on the remote:
   ```
   git ls-remote --tags origin android/$ARGUMENTS
   ```

4. If the tag already exists on the remote:
   a. Stop and ask the user for confirmation:
      ```
      ⚠️  Tag android/$ARGUMENTS already exists on the remote.

      Do you want to delete it and recreate it? (yes / no)
      ```
   b. Wait for the user's response.
      - If the user responds "no" or anything other than "yes": stop and show:
        ```
        ❌ Release cancelled. The existing tag was not modified.
        ```
      - If the user responds "yes": delete the tag from the remote:
        ```
        git push origin :refs/tags/android/$ARGUMENTS
        ```
        Then delete it locally (if it exists):
        ```
        git tag -d android/$ARGUMENTS
        ```
        Inform the user:
        ```
        🗑️  Tag android/$ARGUMENTS has been deleted from remote and local.
        ```
        Then continue to step 5.

5. Update `syncbridge-demo-android/gradle.properties`:
   a. Read the current value of `PROJ_VERSION_CODE` from the file.
   b. Set `PROJ_VERSION_NAME` to $ARGUMENTS without the leading "v" (e.g. "v0.2.0" → "0.2.0").
   c. Increment `PROJ_VERSION_CODE` by 1.
   d. Write both changes to the file.
   e. Verify the result by showing the updated lines.

6. Commit and push the version bump:
   ```
   git add syncbridge-demo-android/gradle.properties
   git commit -m "chore(release): bump Android version to $ARGUMENTS"
   git push origin HEAD
   ```

7. Create the tag locally:
   ```
   git tag android/$ARGUMENTS
   ```

8. Push the tag to the remote to trigger the GitHub Action:
   ```
   git push origin android/$ARGUMENTS
   ```

9. Show a final summary:
   - Run `git ls-remote --tags origin "refs/tags/android/*"` and display the full list of remote Android tags sorted descending by version.
   - Confirm that `android/$ARGUMENTS` appears in the list.
   - Show:
     ```
     ✅ Tag android/$ARGUMENTS successfully published.

     Existing Android tags on remote:
       android/vX.X.X  ← just created
       android/vX.X.X
       ...

     The "Android Release" workflow is now running on GitHub Actions.
     ```
