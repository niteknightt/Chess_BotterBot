package niteknightt.chess.testbot.moveselectors;

import niteknightt.chess.testbot.MoveSelectorException;
import niteknightt.chess.testbot.EvaluatedMove;
import niteknightt.chess.testbot.StockfishClient;
import niteknightt.chess.common.Enums;
import niteknightt.chess.common.GameLogger;
import niteknightt.chess.gameplay.Board;
import niteknightt.chess.gameplay.Move;

import java.util.*;

public abstract class MoveSelector {
    protected Random _random;
    protected StockfishClient _stockfishClient;
    protected Enums.EngineAlgorithm _algorithm;
    protected GameLogger _log;
    protected String _gameId;

    public MoveSelector(Random random, Enums.EngineAlgorithm algorithm, StockfishClient stockfishClient, GameLogger log, String gameId) {
        _random = random;
        _stockfishClient = stockfishClient;
        _algorithm = algorithm;
        _log = log;
        _gameId = gameId;
    }

    public List<EvaluatedMove> getAllMoves(Board board) {
        List<Move> legalMoves = board.getLegalMoves();
        //_log.debug(_gameId, "moveselector", Move.printMovesToString("These are the legal moves", legalMoves));

        String bestMoveUciFormat = "";

        if (legalMoves.size() == 0) {
            return null;
        }

        if (legalMoves.size() == 1) {
            bestMoveUciFormat =  legalMoves.get(0).uciFormat();
            EvaluatedMove evaluatedMove = new EvaluatedMove();
            evaluatedMove.eval = -1000.0;
            evaluatedMove.evalCategory = Enums.MoveEvalCategory.EQUAL;
            evaluatedMove.ismate = false;
            evaluatedMove.matein = 0;
            evaluatedMove.uci = bestMoveUciFormat;
            return Arrays.asList(evaluatedMove);
        }
        else {
            _stockfishClient.setPosition(board.getFen());
            List<EvaluatedMove> movesWithEval = new ArrayList<EvaluatedMove>();
            try {
                Date beforeCall = new Date();
                movesWithEval = _stockfishClient.calcMoves(board.getLegalMoves().size(), 2000, board.whosTurnToGo());
                Date afterCall = new Date();
                long callTime = Math.abs(afterCall.getTime() - beforeCall.getTime());
                //_log.info(_gameId, "moveselector", "instructive;depth=10;moveNumber=" + board.getFullMoveNumber() + ";numPieces=" + board.getNumPiecesOnBoard() + ";numLegalMoves=" + legalMoves.size() + ";timeMs=" + callTime);
            }
            catch (Exception ex) {
                throw new RuntimeException("Exception while calling calcMoves: " + ex.toString());
            }
            if (movesWithEval.size() == 0) {
                throw new RuntimeException("Zero moves from stockfish even though there are legal moves");
                /*
                _log.error(_gameId, "moveselector", "Zero moves from stockfish even though there are legal moves");
                int index = _random.nextInt(legalMoves.size());
                bestMoveUciFormat = legalMoves.get(index).uciFormat();
                EvaluatedMove evaluatedMove = new EvaluatedMove();
                evaluatedMove.eval = -1000.0;
                evaluatedMove.evalCategory = Enums.MoveEvalCategory.EQUAL;
                evaluatedMove.ismate = false;
                evaluatedMove.matein = 0;
                evaluatedMove.uci = bestMoveUciFormat;
                return Arrays.asList(evaluatedMove);
                */
            }
            else {
                if (movesWithEval.size() != legalMoves.size()) {
                    throw new RuntimeException("Number of moves from stockfish (" + movesWithEval.size() + ") is not the same as number of legal moves (" + legalMoves.size() + ")");
                }
                return movesWithEval;
            }
        }
    }

    public abstract Move selectMove(Board board)
            throws MoveSelectorException;
}
