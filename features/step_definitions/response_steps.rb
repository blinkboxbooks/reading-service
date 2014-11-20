Then(/^a valid reason for the failure is returned$/) do
  expect(@response_data.keys).to eq(%w{code description})
end

Then(/^the error reason is "(.*?)"$/) do |state|
  expect(@response_data['code']).to match(/#{state.gsub(" ", "_")}$/)
end

Then(/^each (\w+) has the following attributes:$/) do |_item, table|
  expected_keys = table.rows.map { |r| r[0] }
  table.rows.each do |attribute|
    @response_data['items'].each do |i|
      expect(i.keys).to match_array(expected_keys)
      if (attribute[1] == "Date")
        expect(Time.parse(i[attribute[0]])).to_not raise_error(ArgumentError)
      else
        expect(i[attribute[0]]).to be_a(attribute[1].constantize)
      end
    end
  end
end

Then(/^each (\w+) includes the following attributes:$/) do |_item, table|
  table.rows.each do |attribute|
    @response_data['items'].each do |i|
      expect(i.keys).to include(attribute[0])
      if (attribute[1] == "Date")
        expect(Time.parse(i[attribute[0]])).to_not raise_error(ArgumentError)
      else
        expect(i[attribute[0]]).to be_a(attribute[1].constantize)
      end
    end
  end
end

Then(/^the (\w+) has the following attributes:$/) do |_item, table|
  expected_keys = table.rows.map { |r| r[0] }
  table.rows.each do |attribute|
    expect(@response_data.keys).to match_array(expected_keys)
    if (attribute[1] == "Date")
      expect(Time.parse(@response_data[attribute[0]])).to_not raise_error(ArgumentError)
    else
      expect(@response_data[attribute[0]]).to be_a(attribute[1].constantize)
    end
  end
end

Then(/^the (\w+) includes the following attributes:$/) do |_item, table|
  table.rows.each do |attribute|
    expect(@response_data.keys).to include(attribute[0])
    if (attribute[1] == "Date")
      expect(Time.parse(@response_data[attribute[0]])).to_not raise_error(ArgumentError)
    else
      expect(@response_data[attribute[0]]).to be_a(attribute[1].constantize)
    end
  end
end

Then(/^the request was successful and an empty response is returned$/) do
  Cucumber::Rest::Status.ensure_status(204)
  expect(HttpCapture::RESPONSES.last.body).to be_empty
end
