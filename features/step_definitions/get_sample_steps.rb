When(/^I request (?:a|the) sample book's details$/) do
  sample_isbn =
    if @samples.size >= 1
      @samples.shuffle
    else # request a sample that isn't in the user's library
      data_for_a(:book, which: "is currently available as sample", instances: 1)
    end
  get_library_item(sample_isbn.first['isbn'])
end

And(/^the response contains the following items$/) do |table|
  expected_keys = table.rows.map { |r| r[0] }
  assert_response_contains(@response_data, expected_keys, table.rows)
end

And(/^the ownership status is Sample$/) do
  expect(@response_data['ownership']).to eq('Sample')
end