# ExpandableText

![Maven Central](https://img.shields.io/maven-central/v/io.github.dokar3/expandabletext?style=flat-square&color=%23ea197e)

Expandable text, similar to `Text()` in Jetpack Compose.

[Sample](/sample/src/main/java/io/dokar/expandabletext/sample/MainActivity.kt) screen:

![Screen gif](/images/screen.gif)

# Usage

Add the dependency [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.dokar3/expandabletext/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.dokar3/expandabletext) :

```groovy
implementation 'io.github.dokar3:expandabletext:latest_version'
```

Show some texts:

```kotlin
val text = "Your long text ".repeat(10)
var expanded by remember { mutableStateOf(false) }
ExpandableText(
    expanded = expanded,
    text = text,
    collapsedMaxLines = 2,
    modifier = Modifier
        .animateContentSize()
        .clickable { expanded = !expanded },
    toggle = { Text(text = if (expanded) "Show less" else "Show more") },
)
```

# Known problems

- `toggle` will not be visible if:
    - `overflow` was set to `TextOverflow.Ellipsis`:
      ```kotlin
      ExpandableText(
          // ...
          overflow = TextOverflow.Ellipsis,
      )
      ```
    - `text` is an `AnnoatedString` and a `ParagraphStyle` was applied to the line needed to show the toggle. For example:
      ```kotlin
      val text = buildAnnotatedString {
          withStyle(ParagraphStyle()) {
              append("Some long text...")
          }
      }
      ExpandableText(
        text = text,
        // ...
      )
      ```

# License

```
Copyright 2022 dokar3

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
