Feature: Get library details

  As a books app user
  I want one API call to retrieve my library the first time I open my app
  So that I can have access to my library and books

  Scenario: Using a valid user with one or more books
    Given I am authenticated as a user with one library items
    When I request my library
    Then the request is successful
    And the response is a list containing at least one library items
    And each item has the following attributes:
      | attribute       | type       |
      | isbn            | Integer    |
      | title           | String     |
      | author          | String     |
      | sortableAuthor  | String     |
      | ownership       | String     |
      | readingStatus   | String     |
      | readingPosition | String     |
      | images          | Collection |
      | links           | Collection |
      | addedDate       | DateTime   |


  Scenario: Using a valid user with no books
    Given I am authenticated as a user with no library items
    When I request my library
    Then the request is successful
    And the response is a list that is empty

  @negative
  Scenario: Not authenticated
    Given I am not authenticated
    When I request my library
    Then the request fails because I am unauthorised
