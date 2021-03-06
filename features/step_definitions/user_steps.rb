Given(/^I am authenticated as a user with the (\w+) role$/) do |role|
  @user = data_for_a(:user, which: "has the #{role} role")
  @access_token = get_access_token_for(username: @user['username'], password: @user['password'])
end

Given(/^I am an authenticated user$/) do
  username = random_email
  password = random_password
  u = Blinkbox::User.new(username: username, password: password, server_uri: test_env.servers['auth'])
  u.register
  u.authenticate
  @access_token = get_access_token_for(username: username, password: password)
end

Given(/^I am not authenticated$/) do
  @access_token = "something_totally_fake"
end

private

def random_email
  chars = [*("A".."Z"), *("a".."z"), *("0".."9")]
  "#{chars.sample(40).join}@bbbtest.com"
end

def random_password
  char_groups = ["A".."Z", "a".."z", "0".."9", "!@£$%^&*(){}[]:;'|<,>.?/+=".split(//)]
  char_groups.map { |chars| chars.to_a.sample(5) }.flatten.shuffle.join
end
