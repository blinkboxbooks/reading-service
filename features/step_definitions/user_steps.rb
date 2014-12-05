Given(/^I am authenticated as a user with the (\w+) role$/) do |role|
  @user = data_for_a(:user, which: "has the #{role} role")
  @access_token = get_access_token_for(username: @user['username'], password: @user['password'])
end

Given(/^I am authenticated as a user(?: with (no|#{CAPTURE_INTEGER}) library items?)?$/) do |count|
  authenticate_as_new_user!
  unless count.nil? || count == "no"
    (0..count).each {
      # adding samples for now until we have add book
      books = [*data_for_a(:book, which: "is currently available as sample", instances: count)]
      books.each do |book|
        add_sample(book)
      end
    }
  end
end

Given(/^I am not authenticated$/) do
  @access_token = "something_totally_fake"
end

