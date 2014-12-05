# encoding: utf-8
module KnowsAboutOAuthRequests

  def authenticate!(username, password, client_id = nil, client_secret = nil)
    uri = qualified_uri(:auth, "/oauth2/token")
    params = { grant_type: "password", username: username, password: password }
    params.merge!({ client_id: client_id, client_secret: client_secret }) if client_id
    headers = { "Content-Type" => "application/x-www-form-urlencoded", "Accept" => "application/json" }
    response = http_client.post(uri, body: params, header: headers)
    raise "Test Error: Failed to authenticate user" unless response.status == 200
    auth_props = MultiJson.load(response.body)
    @access_token = auth_props["access_token"]
  end

  def authenticate_as_new_user!(options = {})
    with_client = options[:with_client] || false

    uri = qualified_uri(:auth, "/oauth2/token")
    params = {
      grant_type: "urn:blinkbox:oauth:grant-type:registration",
      first_name: "Testy",
      last_name: "McTest",
      username: random_email,
      password: random_password,
      accepted_terms_and_conditions: true,
      allow_marketing_communications: false
    }
    if with_client
      params.merge!({
        client_name: "Test Client",
        client_brand: "Test Brand",
        client_model: "Test Model",
        client_os: "Test OS"
      })
    end
    headers = { "Content-Type" => "application/x-www-form-urlencoded", "Accept" => "application/json" }
    response = http_client.post(uri, body: params, header: headers)
    raise "Test Error: Failed to register new user" unless response.status == 200
    user_props = MultiJson.load(response.body)
    @access_token = user_props["access_token"]
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

end

World(KnowsAboutOAuthRequests)
