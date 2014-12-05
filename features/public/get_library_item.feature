Feature: Endpoint for retrieving a single library item

  As a books app user
  I want to retrieve information of a library item I have in my library
  So that I may be able to view the library item later on my device

  Scenario: Retrieving library item
    Given I am authenticated as a user
    And I have a library item for isbn 9780007197545
    When I request a library item with isbn 9780007197545
    Then the request is successful
    And I get a library item

#  As a user
#  I want get book endpoint that returns meta data details of a book in my library
#  So that I can confirm if a book is in my library
#
#  Scenario: Looking up a user's book
#    Given I am authenticated as a user
#    And I have three books in my library
#    When I request a book
#    Then the request is successful
#    And the correct book details are returned
#    And the book has the following attributes:
#      | attribute       | type       |
#      | isbn            | Integer    |
#      | title           | String     |
#      | author          | String     |
#      | sortableTitle   | String     |
#      | bookType        | String     |
#      | readingStatus   | String     |
#      | readingPosition | String     |
#      | images          | Collection |
#      | addedTime       | DateTime   |
#
  Scenario: Retrieving library item that does not exist
    Given I am authenticated as a user
    And I have three library items in my library
    When I request a library item which does not exist in my library
    Then the request fails because the library item was not found

  Scenario: Retrieving library item for unauthenticated user
    Given I am not authenticated
    When I request a library item
    Then the request fails because I am unauthorised



