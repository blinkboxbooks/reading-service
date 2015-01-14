Then(/^the error reason is "(.*?)"$/) do |state|
  expect(@response_data['code']).to match(/#{state.gsub(" ", "_")}$/)
end

Then(/^(?:each|the) (\w+) (?:has|includes) the following attributes:$/) do |_item, table|
  expected_keys = table.rows.map { |r| r[0] }
  table.rows.each do |attribute|
    @response_data['items'].each do |i|
      expect(i.keys).to match_array(expected_keys)
      if (attribute[1] == "DateTime")
        expect(DateTime.parse(i[attribute[0]])).to be_a(DateTime)
      else
        expect(i[attribute[0]]).to be_a(attribute[1].constantize)
      end
    end
  end
end

Then(/^the request was successful and an empty response is returned$/) do
  Cucumber::Rest::Status.ensure_status(204)
  expect(HttpCapture::RESPONSES.last.body).to be_empty
end
