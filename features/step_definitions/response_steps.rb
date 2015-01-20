Then(/^the error reason is "(.*?)"$/) do |state|
  expect(@response_data['code']).to match(/#{state.gsub(" ", "_")}$/)
end

Then(/^(?:each|the) (\w+) (?:has|includes) the following attributes:$/) do |_item, table|
  expected_keys = table.rows.map { |r| r[0] }
  @response_data['items'].each do |i|
    assert_response_contains(i, expected_keys, table.rows)
  end
end

Then(/^the request was successful and an empty response is returned$/) do
  Cucumber::Rest::Status.ensure_status(204)
  expect(HttpCapture::RESPONSES.last.body).to be_empty
end

def assert_response_contains(response_hash, expected_hash_keys, datatype_rows)
  datatype_rows.each do |attribute|
    expect(response_hash.keys).to match_array(expected_hash_keys)
    if attribute[1] == "DateTime"
      expect(DateTime.parse(response_hash[attribute[0]])).to be_a(DateTime)
    else
      expect(response_hash[attribute[0]]).to be_a(attribute[1].constantize)
    end
  end
end