module KnowsHowToBuyABook
  def purchase_book!(book)
    add_to_basket!(book)
    purchase_request = {
      "creditCard" => data_for_a(:credit_card, which: "is usable in tests")
    }
    http_post :payment, "/my/payments", purchase_request
  end
end

World(KnowsHowToBuyABook)
