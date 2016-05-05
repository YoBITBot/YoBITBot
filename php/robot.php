<?php
/* YoBIT Trader Bot v0.9 */
/*
	This script performs small buy orders on a select trading pair on YoBIT.
	It automatically re-sells the purchases, up enough to clear transaction fees and make a profit.
	It keeps track of own sells, so it never purchases from itself.
	It recombines sell orders to save on transaction fees.
	It can be customized to trade all the way up to a pre-set target.
	It will only create small buy orders, up to customizable max BTC value.
	It will keep the market active, move the market up, and has proven to be profitable in a group effort.
	It allows you to buy manually, off of the YoBIT website. The bot will perform sales for you at a profitable rate.
	Has been used on ORLY coin, TCR and POST. The more bots run, the faster increase of value.

	WARNING: Default BUY_VALUE has been tested, do not increase too much or you might buy too much to sell.
	Enjoy.
*/
/* Instructions:
	- Configure file to contain API Key and Secret.
	- Upload to a webserver with PHP 5.
	- Create a file called 'noonce.txt' in the same folder.
	- Set the file 'noonce.txt' file writable (chmod 0666)
	- Run the file from webserver or from PHP CLI.
*/
define('API_KEY','YOURYOBITAPIKEY');		/* Put a YoBIT 'info & trade & deposits' API Key here. Withdrawals key is not required, this script will not withdraw.
								 * See https://yobit.net/en/api/keys/ to generate keys. */
define('API_SECRET','YOURYOBITAPISECRET');	/* Put a YoBIT 'info & trade & deposits' API Secret here. This will be used to locally sign and hash trade api requests */
define('NOONCE_FILE','noonce.txt');				/* Put a filename to use for storing noonce ( Noonce: A number that may only be used once, to avoid accident repeat requests ) */
define('TRADING_PAIR','orly_btc');				/* Put the trading pair on YoBIT. We'll be making a combined effort to use this on ORLY, skyrocketing it's value. Such profit */
define('TARGET',1000);						/* The target price in satoshis we are raising this coin's value to */
define('BUY_VALUE',0.0001);					/* The maximum amount of BTC to purchase with in each step, in decimal BTC (must be 0.0001 as that's the minimum trade amount for YoBit) */

/* ==========================================================
 * Start-up and configuration checks
 * ========================================================== */

header("Content-Type: text/plain");

/* Check if noonce file exists and can be written */
if( ( !file_exists( NOONCE_FILE ) && !file_put_contents( NOONCE_FILE, 0 ) ) || !is_writable( NOONCE_FILE ) )
{
	echo "Noonce file ( ". NOONCE_FILE . " ) is not writable. Can not execute.\n\nTo solve: Create " . NOONCE_FILE . " and set chmod 0666 (read-write).";
	exit;
}

/* Check if this version of PHP has CURL support */
if( !function_exists( 'curl_version' ) )
{
	echo "CURL not available. Can not execute.\n\nTo solve: Enable CURL support in PHP.";
	exit;
}

/* Check if this version of PHP has hash_hmac available along with the sha512 algo, needed to sign postdata sent to YoBIT trading API */
if( !function_exists( 'hash_hmac' ) || ( function_exists( 'hash_hmac' ) && !in_array( 'sha512', hash_algos() ) ) )
{
	echo "hash_mac not available. Can not execute.\n\nTo solve: Make sure to run a PHP version with hash_hmac support, with sha512 algo.";
	exit;
}

/* ==========================================================
 * Constant definitions
 * ========================================================== */

/* Bitcoin satoshi value */
define( 'SATOSHI', 1 / 1E8 );

/* Set the primary symbol of the trading pair, roughly assuming correct input in TRADING_PAIR */
define( 'SYMBOL', strtoupper( explode( '_', TRADING_PAIR )[0] ) );

/* ==========================================================
 * Functions
 * ========================================================== */

set_time_limit(7200);
ignore_user_abort(false);

class Trader
{
	public function __construct()
	{
		$this->api_key = API_KEY;
		$this->api_secret = API_SECRET;
	}

	protected function getNoonce()
	{
		$n = intval( trim( @file_get_contents( NOONCE_FILE ) ) ) + 1;
		file_put_contents( NOONCE_FILE, $n );
		return $n;
	}

	/* Shorthand function to format a number in non-scientific, human readable format, up to 8 decimal places */
	public function decimal($n) {
		return sprintf('%.8F', $n);
	}

	public function getPage($url) {
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, FALSE);
		curl_setopt($ch, CURLOPT_HEADER, false);
		curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
		curl_setopt($ch, CURLOPT_URL, $url);
		curl_setopt($ch, CURLOPT_REFERER, $url);
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, TRUE);
		$result = curl_exec($ch);
		curl_close($ch);
		return $result;
	}

	public function apiQuery($method, $req=array())
	{
		/* Set the trade method */
		$req['method'] = $method;

		/* Set the noonce to be used in this request */
		$req['nonce'] = $this->getNoonce();

		/* Generate the POST data string */
		$post_data = http_build_query($req, '', '&');

		/* Generate the signed hash value of postdata */
		$sign = hash_hmac("sha512", $post_data, $this->api_secret);

		/* Add trading key and signature of postdata to the request headers */
		$headers = array(
			'Sign: ' . $sign,
			'Key: ' . $this->api_key,
		);

		/* Create a CURL handler */
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
		curl_setopt($ch, CURLOPT_USERAGENT, 'Mozilla/4.0 (compatible; SMART_API PHP client; '.php_uname('s').'; PHP/'.phpversion().')');
		curl_setopt($ch, CURLOPT_URL, 'https://yobit.net/tapi/');
		curl_setopt($ch, CURLOPT_POSTFIELDS, $post_data);
		curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
		curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
		curl_setopt($ch, CURLOPT_ENCODING , 'gzip');

		/* Send request to API */
		$res = curl_exec($ch);

		/* Check for failure, clean-up curl handler afterwards */
		if( $res === false ) {
			$e = curl_error($ch);
			curl_close($ch);
			echo 'Could not get reply: ' . $e;
			exit;
		} else {
			curl_close($ch);
		}

		/* Decode the JSON response */
		$result = json_decode($res, true);

		/* If result is not valid JSON, something must have gone wrong, in which case, can be ignored by dying off */
		if( !$result ) {
			echo 'Invalid data received, please make sure connection is working and requested API exists';
			exit;
		}

		/* Verbose output about any API errors ( Incorrect noonce, for instance ) */
		if(isset($result['error']) === true) {
				echo 'API Error Message: ' . $result['error'] . ". Response: " . print_r($result, true);
				exit;
		}

		/* We're all-good if we arrived here, return data */
		return $result;
	}

}

$trade = new Trader();

do {
	/* Get the current rate to buy at for trading pair TRADING_PAIR */
 	$rate = json_decode($trade->getPage('https://yobit.net/api/3/depth/'.TRADING_PAIR.'?limit=2'),true)[TRADING_PAIR]['asks'][0][0];

	echo "==========================================================\n";
	echo date('d-m-Y H:i:s')."\n".strtoupper(TRADING_PAIR) . "\n";
	echo "==========================================================\n";
	echo "Rate:\t" . $trade->decimal($rate) . "\n";
	echo "==========================================================\n";

	/* Get available funds to trade with */
	$funds = $trade->apiQuery("getInfo")['return']['funds'];

	/* Filter out all trading pairs with zero balance */
	foreach($funds as $k => $v) if ( $v == 0 ) unset( $funds[$k] );

	/* Get active orders for this pair */
	$orders = $trade->apiQuery("ActiveOrders",
		array(
			'pair'=>TRADING_PAIR
		)
	);

	/* Array that will contain all our sell orders, so we do not buy from ourselves */
	$sellmap = array();

	/* Array to contain selling targets */
	$selltgt = array();

	/* Cycle through active orders */
	foreach( $orders['return'] as $k => $v )
	{
		if( $v['type'] == 'sell' )
		{
			if( isset( $sellmap[ $trade->decimal( $v['rate'] ) ] ) ) $sellmap[ $trade->decimal( $v['rate'] ) ]++; else $sellmap[ $trade->decimal( $v['rate'] ) ] = 1;
			$funds[ strtolower( SYMBOL ) ] = max( 0, $funds[ strtolower( SYMBOL ) ] - $v['amount'] );
			continue;
		}

		if( $v['rate'] != $rate )
		{
			echo "Want to buy at " . $trade->decimal($rate) . ".\nBuying {$v['amount']} " . SYMBOL. " at ". $trade->decimal($v['rate']) . " cancelled.\n";
			$trade->apiQuery("CancelOrder",
				array(
					'order_id'=>$k
				)
			);
		}
		else
		{
			/* Subtract buy order value from available btc */
			$funds['btc'] = max( 0, $funds['btc'] - ( ( $v['amount'] * $v['rate'] ) * 1.002 ) );
		}
	}

	/* Save on transaction fees by combining sell orders */
	foreach( $orders['return'] as $k => $v )
	{
		if( $v['type'] == 'sell' )
		{
			/* Check if we have more than one single sell at this rate, if so, combine them all into one sale to save on transaction fees */
			if( $sellmap[ $trade->decimal( $v['rate'] ) ] > 1 )
			{
				$trade->apiQuery("CancelOrder",
					array(
						'order_id'=>$k
					)
				);

				if( isset( $selltgt[ $trade->decimal( $v['rate'] ) ] ) ) $selltgt[ $trade->decimal( $v['rate'] ) ] += $v['amount']; else $selltgt[ $trade->decimal( $v['rate'] ) ] = $v['amount'];
			}
		}
	}

	/* Resubmit combined sell-orders - if any */
	foreach( $selltgt as $k => $v )
	{
		$trade->apiQuery("Trade",
			array(
				'pair'=>TRADING_PAIR,
				'type'=>'sell',
				'rate'=>$trade->decimal($k),
				'amount'=>$trade->decimal($v)
			)
		);
	}

	/* Check if our buy orders have resulted in actual purchases, and if so, re-sell those just high enough to make a profit */
	if( $funds[ strtolower( SYMBOL ) ] > 100 )
	{
		/* Is the total amount of coins available to sell, big enough to sell? */
		$sell = ( ( ceil( ( $rate * 1E8 ) / 50 ) * 50 ) + 20 ) / 1E8;
		if( $sell * $funds[ strtolower( SYMBOL ) ] > 0.0001 )
		{
			$trade->apiQuery("Trade",
				array(
					'pair'=>TRADING_PAIR,
					'type'=>'sell',
					'rate'=>$trade->decimal( $sell ),
					'amount'=>$trade->decimal( $funds[ strtolower( SYMBOL ) ] )
				)
			);

			echo "Selling " . $trade->decimal( $funds[ strtolower( SYMBOL ) ] ) . " " . SYMBOL . " at " . $trade->decimal( $sell ) . "\n";
			echo "==========================================================\n";
		}
	}

	/* Check if we have enough btc funds to perform any buys at all, otherwise, wait for sells to complete and continue */
	if( $funds['btc'] < 0.0001 )
	{
		echo "BTC Funds: " . $trade->decimal( $funds['btc'] )."\nNot enough BTC to buy with. Waiting for sells to complete.\n";
		echo "==========================================================\n";

		sleep(5);
		continue;
	}

	/* Do not continue to buy any, if:
	 * - We are over the configured max limit
	 * - The current sell order is our own (don't buy from yourself, dummy)
	 */
	if( $rate >= ( TARGET * SATOSHI ) || in_array( $trade->decimal( $rate ), array_keys( $sellmap ) ) )
	{
		sleep(5);
		continue;
	}

	/* We can now perform a new, tiny buy, to keep the market active at all times :) */
	/* Amount of coins to buy off of trading pair TRADING_PAIR */
	$amount = ( min( $funds['btc'], max( 0.0001, BUY_VALUE ) ) / $rate ) + (rand(0,5000000) / 5000000);

	/* Check if fees fit in */
	if( ( $amount * $rate ) / 1002 * 1000 > $funds['btc'] )
	{
		/* Reduce $amount if fees do not fit in */
		$amount = ( min( $funds['btc'], max( 0.0001, BUY_VALUE ) ) / $rate );

		/* Check if fees fit in */
		if( ( $amount * $rate ) / 1002 * 1000 > $funds['btc'] )
		{
			/* Not enough btc to fit fees */
			sleep(5);
			continue;
		}
	}

	echo "Buying " . $trade->decimal($amount) . " " . SYMBOL . " at " . $trade->decimal($rate) . "\n";
	echo "==========================================================\n";

	/* Buy $amount off of the TRADING_PAIR market, at rate $rate */
	$trade->apiQuery("Trade",
		array(
			'pair'=>TRADING_PAIR,
			'type'=>'buy',
			'rate'=>$trade->decimal($rate),
			'amount'=> $trade->decimal($amount)
		)
	);

	/* Wait between 0.5 seconds and 6 seconds before attempting next trade */
	usleep( rand( 5, 60 ) * 100000 );

	echo "\n\n";

} while (true);
?>