Feature: Get library details

  As a books app user
  I want one API call to retrieve my library
  So that I can have access to my library and books

  Scenario Outline: A user with one or more books which are samples
    Given I am an authenticated user
    And I have <count> library items
    When I request my library
    Then the request is successful
    And the response is a list containing <count> library items
    And each item has the following attributes:
      | attribute       | type     |
      | isbn            | String   |
      | title           | String   |
      | author          | String   |
      | sortableAuthor  | String   |
      | ownership       | String   |
      | readingStatus   | String   |
      | readingPosition | Object   |
      | addedDate       | DateTime |
      | images          | Array    |
      | links           | Array    |

  Examples:
    | count |
    | 1     |
    | 2     |
    | 4     |

  Scenario: A valid user with no books
    Given I am an authenticated user
    And I have 0 library items
    When I request my library
    Then the request is successful
    And the response is a list that is empty

  @negative
  Scenario: Not authenticated
    Given I am not authenticated
    When I request my library
    Then the request fails because I am unauthorised