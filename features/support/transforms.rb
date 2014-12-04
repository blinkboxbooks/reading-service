CAPTURE_INTEGER = Transform(/^(?:-?\d+|zero|one|two|three|four|five|six|seven|eight|nine|ten)$/) do |num|
  %w(zero one two three four five six seven eight nine ten).index(num) || num.to_i
end

CAPTURE_NAMED_INDEX = Transform(/^first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth$/) do |value|
  %w(zeroth first second third fourth fifth sixth seventh eighth ninth tenth).index(value) || value.to_i
end
