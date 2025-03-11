fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android detekt

```sh
[bundle exec] fastlane android detekt
```

Run Detekt for code analysis

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android release

```sh
[bundle exec] fastlane android release
```

Create a release build for manual deployment

### android increment_version_name

```sh
[bundle exec] fastlane android increment_version_name
```

Increments the version name

### android internal

```sh
[bundle exec] fastlane android internal
```

Submit a new Internal Build

### android alpha

```sh
[bundle exec] fastlane android alpha
```

Submit a new Alpha Build

### android beta

```sh
[bundle exec] fastlane android beta
```

Submit a new Beta Build

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Deploy a new version to the Google Play

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
