When(/^I request my library$/) do
  get_library
end

Then(/^the response is a list (?:that is empty|containing (#{CAPTURE_INTEGER}) library items)$/) do |count|
  count ||= 0
  expect(@response_data['items'].size).to eq(count)
end
