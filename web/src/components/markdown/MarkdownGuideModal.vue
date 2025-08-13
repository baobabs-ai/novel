<script lang="ts" setup>
import avaterUrl from '@/image/avater.jpg';

const guides = [
  `
# Level 1 Title
## Level 2 Title
### Level 3 Title`,

  `
**Bold**
*Italic*
~~Strikethrough~~
[Link](https://www.esjzone.cc/)`,

  `
- Unordered List
- Unordered List
  - Add spaces to indent
  - Add spaces to indent
- Unordered List`,

  `
1. Ordered List
1. Ordered List
1. Ordered List`,

  `
The following is a separator line, which should be separated from the text by a blank line

---`,
  `
| Left Align | Center | Right Align |
| :- | :-: | -: |
| Text | Text | Text |
| Text | Text | Text |`,

  `
![](${avaterUrl})`,
];
</script>

<template>
  <c-modal title="Formatting Tutorial">
    <n-p>Comments do not support advanced features such as titles, tables, and images.</n-p>
    <n-table :bordered="false" style="table-layout: fixed; width: 100%">
      <thead>
        <tr>
          <th><b>Syntax</b></th>
          <th><b>Preview</b></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="guide in guides" :key="guide">
          <td style="white-space: pre-wrap">
            {{ guide.trimStart() }}
          </td>
          <td>
            <MarkdownView mode="article" :source="guide" />
          </td>
        </tr>
      </tbody>
    </n-table>
  </c-modal>
</template>
