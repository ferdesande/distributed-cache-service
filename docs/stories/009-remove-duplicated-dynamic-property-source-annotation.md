# Story 008: Remove duplicated @DynamicPropertySource

## User Story
As a new developer I see the annotation @DynamicPropertySource forces to a lot of duplication to work with
test containers

## Description
Try to avoid code duplication when Redis test containers tests are run. The annotation @DynamicPropertySource which
configures the system must be handled only by one base class.

## Acceptance Criteria
- [ ] @DynamicPropertySource is set in the smallest number of classes possible

## Technical Considerations
- Leave a hint in case flaky tests appear in the future

## Definition of Done
- [ ] Code implemented
- [ ] Integration tests written and passing
- [ ] Code reviewed
- [ ] Documentation updated

## Dependencies
- None (all features already implemented)
