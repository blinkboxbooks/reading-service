module KnowsAboutAccessTokens
  @@access_tokens = {}

  def get_access_token_for(username: nil, password: nil, server_uri: test_env.servers['auth'])
    if @@access_tokens[username].nil?
      u = Blinkbox::User.new(username: username, password: password, server_uri: server_uri)
      u.authenticate
      fail if u.access_token.nil?
      @@access_tokens[username] = u.access_token
    end

    @@access_tokens[username]
  end
end

World(KnowsAboutAccessTokens)
