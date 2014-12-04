Given(/^I am authenticated as a user with the (\w+) role$/) do |role|
  @user = data_for_a(:user, which: "has the #{role} role")
  @access_token = get_access_token_for(username: @user['username'], password: @user['password'])
end

Given(/^I am authenticated as a user( with no library items)?$/) do |empty_library_user|
  @user = empty_library_user ? data_for_a(:user, which: "is an api user without library items") : data_for_a(:user, which: "is an api user")
  @access_token = get_access_token_for(username: @user['username'], password: @user['password'])
end

Given(/^I am not authenticated$/) do
  @access_token = "something_totally_fake"
end
