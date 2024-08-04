package com.BM.MoneyTransfer.service;

import com.BM.MoneyTransfer.dao.CardDao;
import com.BM.MoneyTransfer.dao.TransactionDao;
import com.BM.MoneyTransfer.dao.UserDao;
import com.BM.MoneyTransfer.dto.enums.Status;
import com.BM.MoneyTransfer.entity.Card;
import com.BM.MoneyTransfer.entity.Transaction;
import com.BM.MoneyTransfer.entity.User;
import com.BM.MoneyTransfer.exception.custom.InsufficientFundsException;
import com.BM.MoneyTransfer.exception.custom.RecipientNotFoundException;
import com.BM.MoneyTransfer.exception.custom.SenderNotFoundException;
import com.BM.MoneyTransfer.exception.custom.UserCardNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Service
@AllArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final CardDao cardDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final CardService cardService;


    @Override
    public Optional<Transaction> findById(Long id) {
        return this.transactionDao.findById(id);
    }

    @Override
    public Page<Transaction> findByEmail(String email, Pageable pageable) {
        return this.transactionDao.findByEmail(email, pageable);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Transaction save(Transaction transaction) {
        // Initially set status to "pending"
        transaction.setStatus(Status.PENDING);

        try {
            validateTransaction(transaction);
            performTransaction(transaction);
            transaction.setStatus(Status.APPROVED);
            return transactionDao.save(transaction);

        }
        catch (InsufficientFundsException e) {
            transaction.setStatus(Status.DENIED);
            return transactionDao.save(transaction);
        } catch (Exception e) {
            transaction.setStatus(Status.DENIED);
        }

        // Save the transaction with its status
        return transaction;
    }


    private void validateTransaction(Transaction transaction) throws RecipientNotFoundException, InsufficientFundsException {

        Card recipientCard = cardService.getCard(transaction.getRecipientCardNumber());
        if (recipientCard == null) {
            throw new RecipientNotFoundException("Recipient not found");
        }
        if (transaction.getAmount().compareTo(cardDao.findCardBalanceByCardNumberForUpdate(transaction.getSenderCardNumber())) > 0) {
            throw new InsufficientFundsException("Insufficient funds");

        }
    }

    private void performTransaction(Transaction transaction) throws SenderNotFoundException,UserCardNotFoundException{
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userDao.getUserWithCards(userName);
        // Sanity Check
        if (userOptional.isEmpty()) {
            throw new SenderNotFoundException("Sender not found");
        }

        User user = userOptional.get();
        Card senderCard = findUserCard(user, transaction.getSenderCardNumber());

        Card recipientCard = cardService.getCard(transaction.getRecipientCardNumber());

        cardService.updateCardBalance(senderCard.getCardNumber(), senderCard.getBalance() - transaction.getAmount());
        cardService.updateCardBalance(recipientCard.getCardNumber(), recipientCard.getBalance() + transaction.getAmount());
    }
    private Card findUserCard(User user, String cardNumber) throws UserCardNotFoundException {
        return user.getCards().stream()
                .filter(card -> card.getCardNumber().equals(cardNumber))
                .findFirst()
                .orElseThrow(() -> new UserCardNotFoundException("The user does not have the specified card"));
    }
}

