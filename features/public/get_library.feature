Feature: Get library details

  As a user
  I want one API call to retrieve my library the first time I open my app
  So that I can have access to my library and books

  Scenario: Using a valid user with one or more books
    Given I am authenticated as a user
   # And I have three library items in my library
    When I request my library
    Then the request is successful
   #And the response is a list containing at least three library items

  # Scenario: Using a valid user with no books
    # Given I am authenticated as a user with no library items
    # And I have no library items in my library
    # When I request my library
    # Then the request is successful
   # And the response is a list that is empty
