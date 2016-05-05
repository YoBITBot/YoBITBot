<?php
/* YoBIT Trader Bot v0.9 */
/*
	This script does nothing more than check for prices at YoBit, and sends an email when a certain price has been reached.
	Configure the address in MAIL_TO.
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
define('ALERT',100);						/* The buy price at which we want to send an email */
define('MAIL_TO','mail@email.com');						/* The buy price at which we want to send an email */

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


	if($rate >= ALERT * SATOSHI)
	{
		mail( MAIL_TO, SYMBOL . " Price alert", SYMBOL . " Price has reached " . $trade->decimal(ALERT*SATOSHI));
		exit;
	}

	/* Check again in 10 seconds */
	sleep(10);

	echo "\n\n";

} while (true);
?>