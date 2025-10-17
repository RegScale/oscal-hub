# OSCAL UX - Accessibility Testing Checklist

This checklist helps verify that the OSCAL UX application meets WCAG 2.1 Level AA standards and Section 508 compliance requirements.

## Table of Contents
- [Keyboard Navigation](#keyboard-navigation)
- [Screen Reader Support](#screen-reader-support)
- [Visual Design](#visual-design)
- [Content Structure](#content-structure)
- [Forms and Interactive Elements](#forms-and-interactive-elements)
- [Dynamic Content](#dynamic-content)
- [Error Handling](#error-handling)
- [Testing Tools](#testing-tools)

---

## Keyboard Navigation

### General Navigation
- [ ] Tab key moves focus forward through all interactive elements
- [ ] Shift+Tab moves focus backward through all interactive elements
- [ ] Focus order follows logical reading order (top to bottom, left to right)
- [ ] Focus is always visible with clear visual indicators
- [ ] No keyboard traps (can always navigate away from any element)
- [ ] Skip navigation link appears when pressing Tab on page load

### Interactive Elements
- [ ] All links are accessible via keyboard
- [ ] All buttons respond to Enter and Space keys
- [ ] All form controls are accessible via keyboard
- [ ] File upload buttons can be activated via keyboard
- [ ] Dropdown menus can be opened and navigated with keyboard
- [ ] Cards/clickable areas respond to Enter key

### Specific Pages
- [ ] **Dashboard**: All operation cards are keyboard accessible
- [ ] **Validate**: Can upload, select options, and validate using only keyboard
- [ ] **Convert**: Format selectors and swap button work with keyboard
- [ ] **Resolve**: All controls accessible via keyboard
- [ ] **Batch**: Operation type toggles work with keyboard (Space/Enter)
- [ ] **History**: Table is navigable, delete buttons accessible

---

## Screen Reader Support

### Landmarks and Structure
- [ ] Page has proper heading hierarchy (h1 → h2 → h3)
- [ ] Main content area has `id="main-content"` for skip link
- [ ] Semantic HTML is used (`<header>`, `<nav>`, `<main>`, `<section>`)
- [ ] Navigation areas have `aria-label` attributes
- [ ] Important sections have `role="region"` with labels

### ARIA Labels
- [ ] All buttons have descriptive `aria-label` attributes
- [ ] All links have descriptive `aria-label` or text
- [ ] Icon-only buttons have text alternatives
- [ ] Decorative icons are marked with `aria-hidden="true"`
- [ ] Form inputs have associated labels

### Live Regions
- [ ] Loading states announce to screen readers (`role="status"`, `aria-live="polite"`)
- [ ] Success/error messages announce to screen readers
- [ ] Progress updates announce to screen readers
- [ ] Dynamic content changes are communicated

### Tables
- [ ] Table headers have `scope="col"` or `scope="row"`
- [ ] Tables have `aria-label` describing their purpose
- [ ] Empty header cells have screen reader-only text
- [ ] Table relationships are clear

---

## Visual Design

### Color and Contrast
- [ ] Text has at least 4.5:1 contrast ratio (WCAG AA)
- [ ] Large text (18pt+) has at least 3:1 contrast ratio
- [ ] UI components have at least 3:1 contrast ratio
- [ ] Color is not the only way to convey information
- [ ] Status indicators use icons + color + text

### Focus Indicators
- [ ] All interactive elements have visible focus styles
- [ ] Focus indicators have sufficient contrast (3:1 minimum)
- [ ] Focus indicators are not hidden or removed
- [ ] Custom focus styles are at least as visible as browser defaults

### Text and Typography
- [ ] Text can be resized up to 200% without loss of content or functionality
- [ ] Line height is at least 1.5x font size
- [ ] Paragraph spacing is at least 2x font size
- [ ] No horizontal scrolling at 320px width
- [ ] Font sizes are not too small (minimum 14px for body text)

---

## Content Structure

### Page Structure
- [ ] Each page has a unique, descriptive title
- [ ] Heading levels are not skipped
- [ ] Content is organized logically
- [ ] Related content is grouped together

### Links
- [ ] Link text describes destination ("Download XML" not "Click here")
- [ ] Links are distinguishable from regular text
- [ ] External links are indicated
- [ ] Links have hover and focus states

---

## Forms and Interactive Elements

### Form Controls
- [ ] All form inputs have associated labels
- [ ] Required fields are marked (not just with color)
- [ ] Field instructions are associated with inputs
- [ ] Error messages are associated with fields
- [ ] Form structure is logical and grouped

### Buttons
- [ ] All buttons have clear, descriptive text or labels
- [ ] Button states are communicated (disabled, loading, active)
- [ ] Icon buttons have text alternatives
- [ ] Button groups have `role="group"` and labels
- [ ] Toggle buttons have `aria-pressed` attribute

### File Upload
- [ ] File input is keyboard accessible
- [ ] Selected file name is announced
- [ ] Supported file types are indicated
- [ ] File size limits are communicated

---

## Dynamic Content

### Loading States
- [ ] Loading indicators have `role="status"`
- [ ] Loading messages are announced to screen readers
- [ ] Progress bars have `aria-label` with current value
- [ ] Spinners do not create motion sickness issues

### Notifications
- [ ] Success messages have `role="status"`
- [ ] Error messages have `role="alert"`
- [ ] Messages persist long enough to be read
- [ ] Messages are not sole reliance on color

### Modals and Dialogs
- [ ] Focus moves to modal when opened
- [ ] Focus is trapped within modal
- [ ] Escape key closes modal
- [ ] Focus returns to trigger element when closed
- [ ] Modal has `role="dialog"` and `aria-label`

---

## Error Handling

### Validation Errors
- [ ] Errors are clearly identified
- [ ] Error messages are specific and helpful
- [ ] Errors are associated with form fields
- [ ] Errors do not rely solely on color
- [ ] Errors are announced to screen readers

### Error Recovery
- [ ] Users can fix errors and resubmit
- [ ] Previous input is preserved when possible
- [ ] Clear instructions on how to fix errors

---

## Testing Tools

### Automated Testing Tools
- [ ] **axe DevTools** - No violations found
- [ ] **WAVE** - No errors, minimal alerts
- [ ] **Lighthouse** - Accessibility score 90+
- [ ] **NVDA/JAWS** - Screen reader testing completed

### Manual Testing
- [ ] Keyboard-only navigation tested
- [ ] Screen reader testing (NVDA on Windows, VoiceOver on Mac)
- [ ] Color contrast analyzer used
- [ ] Testing at 200% zoom
- [ ] Testing at 320px viewport width

### Browser Testing
- [ ] Chrome + NVDA
- [ ] Firefox + NVDA
- [ ] Safari + VoiceOver
- [ ] Edge + Narrator

---

## Page-Specific Checklists

### Dashboard (/)
- [ ] Skip link works and focuses main content
- [ ] All 5 operation cards are keyboard accessible
- [ ] Card hover states work with focus
- [ ] Resource links are properly labeled
- [ ] External link indicators present

### Validate (/validate)
- [ ] File upload is keyboard accessible
- [ ] Model type selector is keyboard accessible
- [ ] Validate button announces status changes
- [ ] Validation results are announced
- [ ] Error/warning items are keyboard accessible
- [ ] Clicking errors highlights lines in code

### Convert (/convert)
- [ ] Source format selector is accessible
- [ ] Target format selector is accessible
- [ ] Swap formats button is accessible
- [ ] Convert button announces progress
- [ ] Download button is accessible
- [ ] Side-by-side editors are properly labeled

### Resolve (/resolve)
- [ ] File upload is accessible
- [ ] Resolve button announces progress
- [ ] Resolution results are announced
- [ ] Control count is announced
- [ ] Download button is accessible

### Batch (/batch)
- [ ] Operation type toggle buttons work with keyboard
- [ ] Toggle state is announced (aria-pressed)
- [ ] Multi-file upload is accessible
- [ ] Progress updates are announced
- [ ] Individual file results are accessible
- [ ] Each result announces success/failure

### History (/history)
- [ ] Refresh button is accessible
- [ ] Statistics cards have proper labels
- [ ] Table is fully navigable via keyboard
- [ ] Delete buttons have descriptive labels
- [ ] Pagination is keyboard accessible
- [ ] Page numbers are announced

---

## Compliance Verification

### WCAG 2.1 Level AA Criteria
- [ ] 1.1.1 Non-text Content
- [ ] 1.3.1 Info and Relationships
- [ ] 1.3.2 Meaningful Sequence
- [ ] 1.4.3 Contrast (Minimum)
- [ ] 1.4.4 Resize Text
- [ ] 2.1.1 Keyboard
- [ ] 2.1.2 No Keyboard Trap
- [ ] 2.4.1 Bypass Blocks
- [ ] 2.4.2 Page Titled
- [ ] 2.4.3 Focus Order
- [ ] 2.4.7 Focus Visible
- [ ] 3.1.1 Language of Page
- [ ] 3.2.1 On Focus
- [ ] 3.2.2 On Input
- [ ] 3.3.1 Error Identification
- [ ] 3.3.2 Labels or Instructions
- [ ] 4.1.2 Name, Role, Value
- [ ] 4.1.3 Status Messages

### Section 508 Requirements
- [ ] Software applications and operating systems
- [ ] Web-based intranet and internet information
- [ ] Telecommunications products
- [ ] Video and multimedia products
- [ ] Self-contained, closed products
- [ ] Desktop and portable computers

---

## Testing Schedule

| Test Type | Frequency | Last Tested | Next Test |
|-----------|-----------|-------------|-----------|
| Automated (axe) | Weekly | - | - |
| Keyboard navigation | Monthly | - | - |
| Screen reader | Quarterly | - | - |
| Color contrast | Quarterly | - | - |
| Full WCAG audit | Annually | - | - |

---

## Known Issues

Document any known accessibility issues here and track their resolution:

| Issue | Severity | Status | Resolution Date |
|-------|----------|--------|-----------------|
| - | - | - | - |

---

## Resources

- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Section 508 Standards](https://www.section508.gov/)
- [WebAIM Resources](https://webaim.org/)
- [axe DevTools](https://www.deque.com/axe/devtools/)
- [WAVE Tool](https://wave.webaim.org/)
- [NVDA Screen Reader](https://www.nvaccess.org/)
- [Chrome Lighthouse](https://developers.google.com/web/tools/lighthouse)

---

**Last Updated**: 2025-10-15
**Next Review**: 2026-01-15
