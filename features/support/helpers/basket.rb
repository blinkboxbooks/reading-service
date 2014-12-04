module KnowsAboutBaskets
  def clear_basket!
    http_delete :basket, "/my/baskets"
  end

  def add_to_basket!(item)
    http_post :basket, "/my/baskets/items", item
  end

  def basket_item_list(process_response: true)
    http_get :basket, "/my/baskets"
    parse_response_data if process_response
  end

  def get_basket_item_by_identifier(identifier, process_response: true)
    http_get :basket, "/my/baskets/items/#{identifier}"
    parse_response_data if process_response
  end

  def remove_basket_item_by_identifier!(identifier, process_response: true)
    http_delete :basket, "/my/baskets/items/#{identifier}"
    parse_response_data if process_response
  end
end

World(KnowsAboutBaskets)
